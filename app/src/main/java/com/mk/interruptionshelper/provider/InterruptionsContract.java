package com.mk.interruptionshelper.provider;

import android.provider.BaseColumns;
import android.net.Uri;

/**
 * Created by MK on 2015/7/7.
 * The contract between the clock provider and desk clock. Contains
 * definitions for the supported URIs and data columns.
 *
 * ClockContract defines the data model of clock related information.
 * This data is stored in a number of tables:
 *
 * The {InterruptionsColumns} table holds the user created interruptions
 * The {InstancesColumns} table holds the current state of each
 * interruption in the InterruptionsColumn table.
 * The {CitiesColumns} table holds all user selectable cities
 */

public final class InterruptionsContract {
    /**
     * This authority is used for writing to or querying from the interruptions helper
     * provider.
     */
    public static final String AUTHORITY = "com.android.interruptionshelper";

    /**
     * This utility class cannot be instantiated
     */
    private InterruptionsContract() {}

    /**
     * Constants for tables with InterruptionsSettings.
     */
    private interface InterruptionsSettingColumns extends BaseColumns {
        /**
         * This string is used to indicate no notification.
         */
        public static final Uri NO_NOTIFICATION_URI = Uri.EMPTY;

        /**
         * This string is used to indicate no notification.
         */
        public static final String NO_NOTIFICATION = NO_NOTIFICATION_URI.toString();

        /**
         * True if interruptions remind should vibrate
         * <p>Type: BOOLEAN</p>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * interruptions label.
         *
         * <p>Type: STRING</p>
         */
        public static final String LABEL = "label";

        /**
         * Audio alert to play when interruptions triggers. Null entry
         * means use system default and entry that equal
         * Uri.EMPTY.toString() means no notification.
         *
         * <p>Type: STRING</p>
         */
        public static final String NOTIFICATION = "notification";

    }

    /**
     * Constants for the Interruptions table, which contains the user created Interruptions.
     */
    protected interface InterruptionsColumns extends InterruptionsSettingColumns, BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/Interruptions");

        /**
         * Hour in 24-hour localtime 0 - 23.
         * <p>Type: INTEGER</p>
         */
        public static final String HOUR = "hour";

        /**
         * Minutes in localtime 0 - 59.
         * <p>Type: INTEGER</p>
         */
        public static final String MINUTES = "minutes";

        /**
         * Time of the Interruption remaining
         * <p>Type: INTEGER</p>
         */
        public static final String DURATION="duration";

        /**
         * Days of the week encoded as a bit set.
         * <p>Type: INTEGER</p>
         *
         * {DaysOfWeek}
         */
        public static final String DAYS_OF_WEEK = "daysofweek";

        /**
         * True if nterruptions is active.
         * <p>Type: BOOLEAN</p>
         */
        public static final String ENABLED = "enabled";

        /**
         * Determine if interruptions is deleted after it has been used.
         * <p>Type: INTEGER</p>
         */
        public static final String DELETE_AFTER_USE = "delete_after_use";
    }

    /**
     * Constants for the Instance table, which contains the state of each interruptions.
     */
    protected interface InstancesColumns extends InterruptionsSettingColumns, BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/instances");

        /**
         * Interruptions state when to show no notification.
         *
         * Can transitions to:
         * LOW_NOTIFICATION_STATE
         */
        public static final int SILENT_STATE = 0;

        /**
         * Interruptions state to show low priority interruptions notification.
         *
         * Can transitions to:
         * HIDE_NOTIFICATION_STATE
         * HIGH_NOTIFICATION_STATE
         * DISMISSED_STATE
         */
        public static final int LOW_NOTIFICATION_STATE = 1;

        /**
         * Interruptions state to hide low priority interruptions notification.
         *
         * Can transitions to:
         * HIGH_NOTIFICATION_STATE
         */
        public static final int HIDE_NOTIFICATION_STATE = 2;

        /**
         * Interruptions state to show high priority interruptions notification.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        public static final int HIGH_NOTIFICATION_STATE = 3;

        /**
         * Interruptions state when interruptions is in snooze.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        public static final int SNOOZE_STATE = 4;

        /**
         * Interruptions state when interruptions is being fired.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * SNOOZED_STATE
         * MISSED_STATE
         */
        public static final int FIRED_STATE = 5;

        /**
         * Interruptions state when interruptions has been missed.
         *
         * Can transitions to:
         * DISMISSED_STATE
         */
        public static final int MISSED_STATE = 6;

        /**
         * Interruptions state when interruptions is done.
         */
        public static final int DISMISSED_STATE = 7;

        /**
         * Interruptions year.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String YEAR = "year";

        /**
         * Interruptions month in year.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String MONTH = "month";

        /**
         * Interruptions day in month.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String DAY = "day";

        /**
         * Interruptions hour in 24-hour localtime 0 - 23.
         * <p>Type: INTEGER</p>
         */
        public static final String HOUR = "hour";

        /**
         * Interruptions minutes in localtime 0 - 59
         * <p>Type: INTEGER</p>
         */
        public static final String MINUTES = "minutes";

        /**
         * Time of the Interruption remaining
         * <p>Type: INTEGER</p>
         */
        public static final String DURATION="duration";

        /**
         * Foreign key to Interruptions table
         * <p>Type: INTEGER (long)</p>
         */
        public static final String INTERRUPTION_ID = "interruption_id";

        /**
         * Interruptions state
         * <p>Type: INTEGER</p>
         */
        public static final String INTERRUPTION_STATE = "interruption_state";
    }

    /**
     * Constants for the Cities table, which contains all selectable cities.
     */
    protected interface CitiesColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/cities");

        /**
         * Primary id for city.
         * <p>Type: STRING</p>
         */
        public static final String CITY_ID = "city_id";

        /**
         * City name.
         * <p>Type: STRING</p>
         */
        public static final String CITY_NAME = "city_name";

        /**
         * Timezone name of city.
         * <p>Type: STRING</p>
         */
        public static final String TIMEZONE_NAME = "timezone_name";

        /**
         * Timezone offset.
         * <p>Type: INTEGER</p>
         */
        public static final String TIMEZONE_OFFSET = "timezone_offset";
    }
}
