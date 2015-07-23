package com.mk.interruptionhelper.provider;

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

public final class InterruptionContract {
    /**
     * This authority is used for writing to or querying from the Interruption helper
     * provider.
     */
    public static final String AUTHORITY = "com.android.Interruptionhelper";

    /**
     * This utility class cannot be instantiated
     */
    private InterruptionContract() {}

    /**
     * Constants for tables with InterruptionSettings.
     */
    private interface InterruptionSettingColumns extends BaseColumns {
        /**
         * This string is used to indicate no notification.
         */
        public static final Uri NO_NOTIFICATION_URI = Uri.EMPTY;

        /**
         * This string is used to indicate no notification.
         */
        public static final String NO_NOTIFICATION = NO_NOTIFICATION_URI.toString();

        /**
         * True if Interruption remind should vibrate
         * <p>Type: BOOLEAN</p>
         */
        public static final String VIBRATE = "vibrate";

        /**
         * Interruption label.
         *
         * <p>Type: STRING</p>
         */
        public static final String LABEL = "label";

        /**
         * Audio alert to play when Interruption triggers. Null entry
         * means use system default and entry that equal
         * Uri.EMPTY.toString() means no notification.
         *
         * <p>Type: STRING</p>
         */
        public static final String NOTIFICATION = "notification";

    }

    /**
     * Constants for the Interruption table, which contains the user created Interruption.
     */
    protected interface InterruptionsColumns extends InterruptionSettingColumns, BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/Interruption");

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
         * Determine if Interruption is deleted after it has been used.
         * <p>Type: INTEGER</p>
         */
        public static final String DELETE_AFTER_USE = "delete_after_use";
    }

    /**
     * Constants for the Instance table, which contains the state of each Interruption.
     */
    protected interface InstancesColumns extends InterruptionSettingColumns, BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/instances");

        /**
         * Interruption state when to show no notification.
         *
         * Can transitions to:
         * LOW_NOTIFICATION_STATE
         */
        public static final int SILENT_STATE = 0;

        /**
         * Interruption state to show low priority Interruption notification.
         *
         * Can transitions to:
         * HIDE_NOTIFICATION_STATE
         * HIGH_NOTIFICATION_STATE
         * DISMISSED_STATE
         */
        public static final int LOW_NOTIFICATION_STATE = 1;

        /**
         * Interruption state to hide low priority Interruption notification.
         *
         * Can transitions to:
         * HIGH_NOTIFICATION_STATE
         */
        public static final int HIDE_NOTIFICATION_STATE = 2;

        /**
         * Interruption state to show high priority Interruption notification.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        public static final int HIGH_NOTIFICATION_STATE = 3;

        /**
         * Interruption state when Interruption is in snooze.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * FIRED_STATE
         */
        public static final int SNOOZE_STATE = 4;

        /**
         * Interruption state when Interruption is being fired.
         *
         * Can transitions to:
         * DISMISSED_STATE
         * SNOOZED_STATE
         * MISSED_STATE
         */
        public static final int FIRED_STATE = 5;

        /**
         * Interruption state when Interruption has been missed.
         *
         * Can transitions to:
         * DISMISSED_STATE
         */
        public static final int MISSED_STATE = 6;

        /**
         * Interruption state when Interruption is done.
         */
        public static final int DISMISSED_STATE = 7;

        /**
         * Interruption year.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String YEAR = "year";

        /**
         * Interruption month in year.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String MONTH = "month";

        /**
         * Interruption day in month.
         *
         * <p>Type: INTEGER</p>
         */
        public static final String DAY = "day";

        /**
         * Interruption hour in 24-hour localtime 0 - 23.
         * <p>Type: INTEGER</p>
         */
        public static final String HOUR = "hour";

        /**
         * Interruption minutes in localtime 0 - 59
         * <p>Type: INTEGER</p>
         */
        public static final String MINUTES = "minutes";

        /**
         * Time of the Interruption remaining
         * <p>Type: INTEGER</p>
         */
        public static final String DURATION="duration";

        /**
         * Foreign key to Interruption table
         * <p>Type: INTEGER (long)</p>
         */
        public static final String INTERRUPTION_ID = "interruption_id";

        /**
         * Interruption state
         * <p>Type: INTEGER</p>
         */
        public static final String INTERRUPTION_STATE = "interruption_state";
    }
}
