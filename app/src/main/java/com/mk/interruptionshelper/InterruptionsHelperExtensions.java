package com.mk.interruptionshelper;

import android.content.Context;

import com.mk.interruptionshelper.provider.InterruptionsAlarm;

/**
 * Created by dhdev_000 on 2015/7/21.
 * InterruptionsHelperExtensions
 */
public interface InterruptionsHelperExtensions {
        /**
         * Notify paired device that a new alarm has been created on the phone, so that the alarm can be
         * synced to the device.
         *
         * @param context  the application context.
         * @param newAlarm the alarm to add.
         */
        public void addAlarm(Context context, InterruptionsAlarm newAlarm);

        /**
         * Notify paired device that an alarm has been deleted from the phone so that it can also be
         * deleted from the device.
         *
         * @param context the application context.
         * @param alarmId the alarm id of the alarm to delete.
         */
        public void deleteAlarm(Context context, long alarmId);
}
