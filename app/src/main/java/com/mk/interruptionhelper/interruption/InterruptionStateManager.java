package com.mk.interruptionhelper.interruption;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.mk.interruptionhelper.provider.Interruption;
import com.mk.interruptionhelper.provider.InterruptionInstance;
import com.mk.interruptionhelper.util.LogUtils;

import java.util.Calendar;
import java.util.List;

/*
 * Created by dhdev_000 on 2015/7/30.
 *  * This class handles all the state changes for alarm instances. You need to
 * register all alarm instances with the state manager if you want them to
 * be activated. If a major time change has occurred (ie. TIMEZONE_CHANGE, TIMESET_CHANGE),
 * then you must also re-register instances to fix their states.
 *
 * Please see {@link #registerInstance) for special transitions when major time changes
 * occur.
 *
 * Following states:
 *
 * SILENT_STATE:
 * This state is used when the alarm is activated, but doesn't need to display anything. It
 * is in charge of changing the alarm instance state to a LOW_NOTIFICATION_STATE.
 *
 * LOW_NOTIFICATION_STATE:
 * This state is used to notify the user that the alarm will go off
 * {@link InterruptionInstance.LOW_NOTIFICATION_HOUR_OFFSET}. This
 * state handles the state changes to HIGH_NOTIFICATION_STATE, HIDE_NOTIFICATION_STATE and
 * DISMISS_STATE.
 *
 * HIDE_NOTIFICATION_STATE:
 * This is a transient state of the LOW_NOTIFICATION_STATE, where the user wants to hide the
 * notification. This will sit and wait until the HIGH_PRIORITY_NOTIFICATION should go off.
 *
 * HIGH_NOTIFICATION_STATE:
 * This state behaves like the LOW_NOTIFICATION_STATE, but doesn't allow the user to hide it.
 * This state is in charge of triggering a FIRED_STATE or DISMISS_STATE.
 *
 * SNOOZED_STATE:
 * The SNOOZED_STATE behaves like a HIGH_NOTIFICATION_STATE, but with a different message. It
 * also increments the alarm time in the instance to reflect the new snooze time.
 *
 * FIRED_STATE:
 * The FIRED_STATE is used when the alarm is firing. It will start the AlarmService, and wait
 * until the user interacts with the alarm via SNOOZED_STATE or DISMISS_STATE change. If the user
 * doesn't then it might be change to MISSED_STATE if auto-silenced was enabled.
 *
 * MISSED_STATE:
 * The MISSED_STATE is used when the alarm already fired, but the user could not interact with
 * it. At this point the alarm instance is dead and we check the parent alarm to see if we need
 * to disable or schedule a new alarm_instance. There is also a notification shown to the user
 * that he/she missed the alarm and that stays for
 * {@link InterruptionInstance.MISSED_TIME_TO_LIVE_HOUR_OFFSET} or until the user acknownledges it.
 *
 * DISMISS_STATE:
 * This is really a transient state that will properly delete the alarm instance. Use this state,
 * whenever you want to get rid of the alarm instance. This state will also check the alarm
 * parent to see if it should disable or schedule a new alarm instance.
 */
public class InterruptionStateManager {
    // These defaults must match the values in res/xml/settings.xml
    private static final String DEFAULT_SNOOZE_MINUTES = "10";

    // Intent action to trigger an instance state change.
    public static final String CHANGE_STATE_ACTION = "change_state";

    // Intent action to show the interruption and dismiss the instance
    public static final String SHOW_AND_DISMISS_INTERRUPTION_ACTION = "show_and_dismiss_interruption";

    // Intent action for an InterruptionManager interruption serving only to set the next interruption indicators
    private static final String INDICATOR_ACTION = "indicator";

    // Extra key to set the desired state change.
    public static final String INTERRUPTION_STATE_EXTRA = "intent.extra.interruption.state";

    // Extra key to set the global broadcast id.
    private static final String INTERRUPTION_GLOBAL_ID_EXTRA = "intent.extra.interruption.global.id";

    // Intent category tag used when schedule state change intents in interruption manager.
    public static final String INTERRUPTION_MANAGER_TAG = "INTERRUPTION_MANAGER";

    // Buffer time in seconds to fire alarm instead of marking it missed.
    public static final int INTERRUPTION_FIRE_BUFFER = 15;



    /**
     * This will set the alarm instance to the SILENT_STATE and update
     * the application notifications and schedule any state changes that need
     * to occur in the future.
     *
     * @param context application context
     * @param instance to set state to
     */
    public static void setDismissState(Context context, InterruptionInstance instance) {
        LogUtils.v("Setting dismissed state to instance " + instance.mId);

        // Remove all other timers and notifications associated to it
        unregisterInstance(context, instance);

        // Check parent if it needs to reschedule, disable or delete itself
        if (instance.mInterruptionId != null) {
//            updateParentInterruption(context, instance);
        }

        // Delete instance as it is not needed anymore
        InterruptionInstance.deleteInstance(context.getContentResolver(), instance.mId);

        // Instance is not valid anymore, so find next alarm that will fire and notify system
//        updateNextInterruption(context);
    }

    /**
     * This will not change the state of instance, but remove it's notifications and
     * alarm timers.
     *
     * @param context application context
     * @param instance to unregister
     */
    public static void unregisterInstance(Context context, InterruptionInstance instance) {
        // Stop alarm if this instance is firing it
//        InterruptionService.stopAlarm(context, instance);
//        InterruptionNotifications.clearNotification(context, instance);
//        cancelScheduledInstance(context, instance);
    }

    /**
     * This registers the AlarmInstance to the state manager. This will look at the instance
     * and choose the most appropriate state to put it in. This is primarily used by new
     * alarms, but it can also be called when the system time changes.
     *
     * Most state changes are handled by the states themselves, but during major time changes we
     * have to correct the alarm instance state. This means we have to handle special cases as
     * describe below:
     *
     * <ul>
     *     <li>Make sure all dismissed alarms are never re-activated</li>
     *     <li>Make sure firing alarms stayed fired unless they should be auto-silenced</li>
     *     <li>Missed instance that have parents should be re-enabled if we went back in time</li>
     *     <li>If alarm was SNOOZED, then show the notification but don't update time</li>
     *     <li>If low priority notification was hidden, then make sure it stays hidden</li>
     * </ul>
     *
     * If none of these special case are found, then we just check the time and see what is the
     * proper state for the instance.
     *
     * @param context application context
     * @param instance to register
     */
    public static void registerInstance(Context context, InterruptionInstance instance,
                                        boolean updateNextAlarm) {
        Calendar currentTime = Calendar.getInstance();
        Calendar alarmTime = instance.getInterruptionTime();
        Calendar timeoutTime = instance.getTimeout(context);
        Calendar lowNotificationTime = instance.getLowNotificationTime();
        Calendar highNotificationTime = instance.getHighNotificationTime();
        Calendar missedTTL = instance.getMissedTimeToLive();

        // Handle special use cases here
        if (instance.mInterruptionState == InterruptionInstance.DISMISSED_STATE) {
            // This should never happen, but add a quick check here
            LogUtils.e("Alarm Instance is dismissed, but never deleted");
            setDismissState(context, instance);
            return;
        } else if (instance.mInterruptionState == InterruptionInstance.FIRED_STATE) {
            // Keep alarm firing, unless it should be timed out
            boolean hasTimeout = timeoutTime != null && currentTime.after(timeoutTime);
            if (!hasTimeout) {
//                setFiredState(context, instance);
                return;
            }
        } else if (instance.mInterruptionState == InterruptionInstance.MISSED_STATE) {
            if (currentTime.before(alarmTime)) {
                if (instance.mInterruptionId == null) {
                    // This instance parent got deleted (ie. deleteAfterUse), so
                    // we should not re-activate it.-
                    setDismissState(context, instance);
                    return;
                }

                // TODO: This will re-activate missed snoozed alarms, but will
                // use our normal notifications. This is not ideal, but very rare use-case.
                // We should look into fixing this in the future.

                // Make sure we re-enable the parent alarm of the instance
                // because it will get activated by by the below code
                ContentResolver cr = context.getContentResolver();
                Interruption interruption = Interruption.getInterruption(cr, instance.mInterruptionId);
                interruption.enabled = true;
                Interruption.updateInterruption(cr, interruption);
            }
        }

        // Fix states that are time sensitive
        if (currentTime.after(missedTTL)) {
            // Alarm is so old, just dismiss it
            setDismissState(context, instance);
        } else if (currentTime.after(alarmTime)) {
            // There is a chance that the TIME_SET occurred right when the alarm should go off, so
            // we need to add a check to see if we should fire the alarm instead of marking it
            // missed.
            Calendar interruptionBuffer = Calendar.getInstance();
            interruptionBuffer.setTime(alarmTime.getTime());
            interruptionBuffer.add(Calendar.SECOND, INTERRUPTION_FIRE_BUFFER);
            if (currentTime.before(interruptionBuffer)) {
//                setFiredState(context, instance);
            } else {
//                setMissedState(context, instance);
            }
        } else if (instance.mInterruptionState == InterruptionInstance.SNOOZE_STATE) {
            // We only want to display snooze notification and not update the time,
            // so handle showing the notification directly
//            InterruptionNotifications.showSnoozeNotification(context, instance);
//            scheduleInstanceStateChange(context, instance.getInterruptionTime(),
//                    instance, InterruptionInstance.FIRED_STATE);
//        } else if (currentTime.after(highNotificationTime)) {
//            setHighNotificationState(context, instance);
//        } else if (currentTime.after(lowNotificationTime)) {
//            // Only show low notification if it wasn't hidden in the past
//            if (instance.mAlarmState == AlarmInstance.HIDE_NOTIFICATION_STATE) {
//                setHideNotificationState(context, instance);
//            } else {
//                setLowNotificationState(context, instance);
//            }
//        } else {
//            // Alarm is still active, so initialize as a silent alarm
//            setSilentState(context, instance);
        }

        // The caller prefers to handle updateNextAlarm for optimization
        if (updateNextAlarm) {
//            updateNextAlarm(context);
        }
    }

    /**
     * This will delete and unregister all instances associated with alarmId, without affect
     * the alarm itself. This should be used whenever modifying or deleting an alarm.
     *
     * @param context application context
     * @param alarmId to find instances to delete.
     */
    public static void deleteAllInstances(Context context, long alarmId) {
        ContentResolver cr = context.getContentResolver();
        List<InterruptionInstance> instances = InterruptionInstance.getInstancesByInterruptionId(cr, alarmId);
        for (InterruptionInstance instance : instances) {
            unregisterInstance(context, instance);
            InterruptionInstance.deleteInstance(context.getContentResolver(), instance.mId);
        }
//        updateNextAlarm(context);
    }
}
