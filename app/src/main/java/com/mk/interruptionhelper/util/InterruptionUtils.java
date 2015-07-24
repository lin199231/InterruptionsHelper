package com.mk.interruptionhelper;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

import android.app.TimePickerDialog;
import com.mk.interruptionhelper.provider.Interruption;
import com.mk.interruptionhelper.provider.InterruptionInstance;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by dhdev_000 on 2015/7/24.
 * Static utility methods for Interruptions.
 */
public class InterruptionUtils {
    public static final String FRAG_TAG_TIME_PICKER = "time_dialog";

    public static String getFormattedTime(Context context, Calendar time) {
        String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }

    public static String getInterruptionText(Context context, InterruptionInstance instance) {
        String interruptionTimeStr = getFormattedTime(context, instance.getInterruptionTime());
        return !instance.mLabel.isEmpty() ? interruptionTimeStr + " - " + instance.mLabel
                : interruptionTimeStr;
    }

    /**
     * Show the time picker dialog. This is called from AlarmClockFragment to set alarm.
     * @param fragment The calling fragment (which is also a onTimeSetListener),
     *                 we use it as the target fragment of the TimePickerFragment, so later the
     *                 latter can retrieve it and set it as its onTimeSetListener when the fragment
     *                 is recreated.
     * @param interruption The clicked alarm, it can be null if user was clicking the fab instead.
     */
    public static void showTimeEditDialog(Fragment fragment, final Interruption interruption) {
        final FragmentManager manager = fragment.getFragmentManager();
        final FragmentTransaction ft = manager.beginTransaction();
        final Fragment prev = manager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();
/*        final TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTargetFragment(fragment, 0);
        timePickerFragment.setOnTimeSetListener((TimePickerDialog.OnTimeSetListener) fragment);
        timePickerFragment.setAlarm(alarm);
        timePickerFragment.show(manager, FRAG_TAG_TIME_PICKER);*/
    }

    /**
     * format "Interruption set for 2 days 7 hours and 53 minutes from
     * now"
     */
    private static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                        context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                        context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                        context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                (dispHour ? 2 : 0) |
                (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    public static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }
}
