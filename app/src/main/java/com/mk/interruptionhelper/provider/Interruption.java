package com.mk.interruptionhelper.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.content.Intent;
import android.content.Context;
import android.media.RingtoneManager;

import com.mk.interruptionshelper.R;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by MK on 2015/7/7.
 */
public final class Interruption implements Parcelable, InterruptionContract.InterruptionsColumns{
    /**
     * Interruption start with an invalid id when it hasn't been saved to the database.
     */
    public static final long INVALID_ID = -1;

    /**
     * The default sort order for this table
     */
    private static final String DEFAULT_SORT_ORDER =
            HOUR + ", " +
            MINUTES + " ASC" + ", " +
            _ID + " DESC";

    private static final String[] QUERY_COLUMNS = {
            _ID,
            HOUR,
            MINUTES,
            DURATION,
            DAYS_OF_WEEK,
            ENABLED,
            VIBRATE,
            LABEL,
            NOTIFICATION,
            DELETE_AFTER_USE
    };

    /**
     * These save calls to cursor.getColumnIndexOrThrow()
     * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
     */
    private static final int ID_INDEX = 0;
    private static final int HOUR_INDEX = 1;
    private static final int MINUTES_INDEX = 2;
    private static final int DURATION_INDEX = 3;
    private static final int DAYS_OF_WEEK_INDEX = 4;
    private static final int ENABLED_INDEX = 5;
    private static final int VIBRATE_INDEX = 6;
    private static final int LABEL_INDEX = 7;
    private static final int NOTIFICATION_INDEX = 8;
    private static final int DELETE_AFTER_USE_INDEX = 9;

    private static final int COLUMN_COUNT = DELETE_AFTER_USE_INDEX + 1;

    /*
     * create a Context to save Interruption
     */
    public static ContentValues createContentValues(Interruption interruption) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (interruption.id != INVALID_ID) {
            values.put(InterruptionContract.InterruptionsColumns._ID, interruption.id);
        }

        values.put(ENABLED, interruption.enabled ? 1 : 0);
        values.put(HOUR, interruption.hour);
        values.put(MINUTES, interruption.minutes);
        values.put(DURATION, interruption.duration);
        values.put(DAYS_OF_WEEK, interruption.daysOfWeek.getBitSet());
        values.put(VIBRATE, interruption.vibrate ? 1 : 0);
        values.put(LABEL, interruption.label);
        values.put(DELETE_AFTER_USE, interruption.deleteAfterUse);
        if (interruption.alert == null) {
            // We want to put null, so default interruption changes
            values.putNull(NOTIFICATION);
        } else {
            values.put(NOTIFICATION, interruption.alert.toString());
        }

        return values;
    }

    public static Intent createIntent(String action, long interruptionId) {
        return new Intent(action).setData(getUri(interruptionId));
    }

    public static Intent createIntent(Context context, Class<?> cls, long interruptionId) {
        return new Intent(context, cls).setData(getUri(interruptionId));
    }

    public static Uri getUri(long interruptionId) {
        return ContentUris.withAppendedId(CONTENT_URI, interruptionId);
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    /**
     * Get interruption cursor loader for all interruptions.
     *
     * @param context to query the database.
     * @return cursor loader with all the interruptions.
     */
    public static CursorLoader getInterruptionsCursorLoader(Context context) {
        return new CursorLoader(context, InterruptionContract.InterruptionsColumns.CONTENT_URI,
                QUERY_COLUMNS, null, null, DEFAULT_SORT_ORDER);
    }

    /**
     * Get interruption by id.
     *
     * @param contentResolver to perform the query on.
     * @param interruptionId for the desired interruption.
     * @return interruption if found, null otherwise
     */
    public static Interruption getInterruption(ContentResolver contentResolver, long interruptionId) {
        Cursor cursor = contentResolver.query(getUri(interruptionId), QUERY_COLUMNS, null, null, null);
        Interruption result = null;
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                result = new Interruption(cursor);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    /**
     * Get all Interruptions given conditions.
     *
     * @param contentResolver to perform the query on.
     * @param selection A filter declaring which rows to return, formatted as an
     *         SQL WHERE clause (excluding the WHERE itself). Passing null will
     *         return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in the order that they
     *         appear in the selection. The values will be bound as Strings.
     * @return list of interruptions matching where clause or empty list if none found.
     */
    public static List<Interruption> getInterruptions(ContentResolver contentResolver,
                                        String selection, String ... selectionArgs) {
        Cursor cursor  = contentResolver.query(CONTENT_URI, QUERY_COLUMNS,
                selection, selectionArgs, null);
        List<Interruption> result = new LinkedList<Interruption>();
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(new Interruption(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static Interruption addInterruption(ContentResolver contentResolver, Interruption interruption) {
        ContentValues values = createContentValues(interruption);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        interruption.id = getId(uri);
        return interruption;
    }

    public static boolean updateInterruption(ContentResolver contentResolver, Interruption interruption) {
        if (interruption.id == Interruption.INVALID_ID) return false;
        ContentValues values = createContentValues(interruption);
        long rowsUpdated = contentResolver.update(getUri(interruption.id), values, null, null);
        return rowsUpdated == 1;
    }

    public static boolean deleteInterruption(ContentResolver contentResolver, long interruptionId) {
        if (interruptionId == INVALID_ID) return false;
        int deletedRows = contentResolver.delete(getUri(interruptionId), "", null);
        return deletedRows == 1;
    }

    public static final Parcelable.Creator<Interruption> CREATOR = new Parcelable.Creator<Interruption>() {
        public Interruption createFromParcel(Parcel p) {
            return new Interruption(p);
        }

        public Interruption[] newArray(int size) {
            return new Interruption[size];
        }
    };

    // Public fields
    // TODO: Refactor instance names
    public long id;
    public boolean enabled;
    public int hour;
    public int minutes;
    public int duration;
    public DaysOfWeek daysOfWeek;
    public boolean vibrate;
    public String label;
    public Uri alert;
    public boolean deleteAfterUse;


    // Creates a default interruption at the current time.
    //Interruption的构造函数
    public Interruption() {
        this(0, 0);
    }

    public Interruption(int hour, int minutes) {
        this.id = INVALID_ID;
        this.hour = hour;
        this.minutes = minutes;
        this.vibrate = true;
        this.daysOfWeek = new DaysOfWeek(0);
        this.label = "";
        this.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        this.deleteAfterUse = false;
    }

    public Interruption(Cursor c) {
        id = c.getLong(ID_INDEX);
        enabled = c.getInt(ENABLED_INDEX) == 1;
        hour = c.getInt(HOUR_INDEX);
        minutes = c.getInt(MINUTES_INDEX);
        duration = c.getInt(DURATION_INDEX);
        daysOfWeek = new DaysOfWeek(c.getInt(DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(VIBRATE_INDEX) == 1;
        label = c.getString(LABEL_INDEX);
        deleteAfterUse = c.getInt(DELETE_AFTER_USE_INDEX) == 1;

        if (c.isNull(NOTIFICATION_INDEX)) {
            // Should we be saving this with the current ringtone or leave it null
            // so it changes when user changes default ringtone?
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            alert = Uri.parse(c.getString(NOTIFICATION_INDEX));
        }
    }

    Interruption(Parcel p) {
        id = p.readLong();
        enabled = p.readInt() == 1;
        hour = p.readInt();
        minutes = p.readInt();
        daysOfWeek = new DaysOfWeek(p.readInt());
        vibrate = p.readInt() == 1;
        label = p.readString();
        alert = (Uri) p.readParcelable(null);
        deleteAfterUse = p.readInt() == 1;
    }

    public String getLabelOrDefault(Context context) {
        if (label == null || label.length() == 0) {
            return context.getString(R.string.default_label);
        }
        return label;
    }

    //持久化存储
    public void writeToParcel(Parcel p, int flags) {
        p.writeLong(id);
        p.writeInt(enabled ? 1 : 0);
        p.writeInt(hour);
        p.writeInt(minutes);
        p.writeInt(daysOfWeek.getBitSet());
        p.writeInt(vibrate ? 1 : 0);
        p.writeString(label);
        p.writeParcelable(alert, flags);
        p.writeInt(deleteAfterUse ? 1 : 0);
    }

    public int describeContents() {
        return 0;
    }

    public InterruptionInstance createInstanceAfter(Calendar time) {
        Calendar nextInstanceTime = Calendar.getInstance();
        nextInstanceTime.set(Calendar.YEAR, time.get(Calendar.YEAR));
        nextInstanceTime.set(Calendar.MONTH, time.get(Calendar.MONTH));
        nextInstanceTime.set(Calendar.DAY_OF_MONTH, time.get(Calendar.DAY_OF_MONTH));
        nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        nextInstanceTime.set(Calendar.MINUTE, minutes);
        nextInstanceTime.set(Calendar.SECOND, 0);
        nextInstanceTime.set(Calendar.MILLISECOND, 0);

        // If we are still behind the passed in time, then add a day
        if (nextInstanceTime.getTimeInMillis() <= time.getTimeInMillis()) {
            nextInstanceTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        // The day of the week might be invalid, so find next valid one
        int addDays = daysOfWeek.calculateDaysToNextInterruption(nextInstanceTime);
        if (addDays > 0) {
            nextInstanceTime.add(Calendar.DAY_OF_WEEK, addDays);
        }

        InterruptionInstance result = new InterruptionInstance(nextInstanceTime, id);
        result.mVibrate = vibrate;
        result.mLabel = label;
        result.mNotification = alert;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Interruption)) return false;
        final Interruption other = (Interruption) o;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    //以字符串形式显示Interruption的数据
    @Override
    public String toString() {
        return "Interruption{" +
                "alert=" + alert +
                ", id=" + id +
                ", enabled=" + enabled +
                ", hour=" + hour +
                ", minutes=" + minutes +
                ", duration=" + duration +
                ", daysOfWeek=" + daysOfWeek +
                ", vibrate=" + vibrate +
                ", label='" + label + '\'' +
                ", deleteAfterUse=" + deleteAfterUse +
                '}';
    }
}
