package com.mk.interruptionhelper.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.mk.interruptionhelper.LogUtils;

import java.util.Calendar;

/**
 * Created by dhdev_000 on 2015/7/23.
 * Helper class for opening the database from multiple providers.  Also provides
 * some common functionality.
 */
public class InterruptionDatabaseHelper extends SQLiteOpenHelper {
    /**
     * Introduce:
     * Interruption Database
     * InterruptionAlarm_instances table
     */
    private static final int VERSION = 1;

    /**
     * Added default interruption alarm
     */
    // This creates a default interruption at 12:00-13:00 for every Mon,Tue,Wed,Thu,Fri
    private static final String DEFAULT_INTERRUPTION_1 = "(12, 00, 31, 0, 0, '', NULL, 0);";

    // Database and table names
    static final String DATABASE_NAME = "interruptions.db";
    static final String OLD_INTERRUPTIONS_TABLE_NAME = "interruptions";
    static final String INTERRUPTIONS_TABLE_NAME = "interruption_templates";
    static final String INSTANCES_TABLE_NAME = "interruption_instances";
    static final String CITIES_TABLE_NAME = "selected_cities";

    private static void createAlarmsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + INTERRUPTIONS_TABLE_NAME + " (" +
                InterruptionContract.InterruptionsColumns._ID + " INTEGER PRIMARY KEY," +
                InterruptionContract.InterruptionsColumns.HOUR + " INTEGER NOT NULL, " +
                InterruptionContract.InterruptionsColumns.MINUTES + " INTEGER NOT NULL, " +
                InterruptionContract.InterruptionsColumns.DAYS_OF_WEEK + " INTEGER NOT NULL, " +
                InterruptionContract.InterruptionsColumns.ENABLED + " INTEGER NOT NULL, " +
                InterruptionContract.InterruptionsColumns.VIBRATE + " INTEGER NOT NULL, " +
                InterruptionContract.InterruptionsColumns.LABEL + " TEXT NOT NULL, " +
                InterruptionContract.InterruptionsColumns.NOTIFICATION + " TEXT, " +
                InterruptionContract.InterruptionsColumns.DELETE_AFTER_USE + " INTEGER NOT NULL DEFAULT 0);");
        LogUtils.i("Interruptions Table created");
    }

    private static void createInstanceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + INSTANCES_TABLE_NAME + " (" +
                InterruptionContract.InstancesColumns._ID + " INTEGER PRIMARY KEY," +
                InterruptionContract.InstancesColumns.YEAR + " INTEGER NOT NULL, " +
                InterruptionContract.InstancesColumns.MONTH + " INTEGER NOT NULL, " +
                InterruptionContract.InstancesColumns.DAY + " INTEGER NOT NULL, " +
                InterruptionContract.InstancesColumns.HOUR + " INTEGER NOT NULL, " +
                InterruptionContract.InstancesColumns.MINUTES + " INTEGER NOT NULL, " +
                InterruptionContract.InstancesColumns.VIBRATE + " INTEGER NOT NULL, " +
                InterruptionContract.InstancesColumns.LABEL + " TEXT NOT NULL, " +
                InterruptionContract.InstancesColumns.NOTIFICATION + " TEXT, " +
                InterruptionContract.InstancesColumns.INTERRUPTION_STATE + " INTEGER NOT NULL, " +
                InterruptionContract.InstancesColumns.INTERRUPTION_ID + " INTEGER REFERENCES " +
                INTERRUPTIONS_TABLE_NAME + "(" + InterruptionContract.InterruptionsColumns._ID + ") " +
                "ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");
        LogUtils.i("Instance table created");
    }

    private Context mContext;

    public InterruptionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAlarmsTable(db);
        createInstanceTable(db);

        // insert default alarms
        LogUtils.i("Inserting default alarms");
        String cs = ", "; //comma and space
        String insertMe = "INSERT INTO " + INTERRUPTIONS_TABLE_NAME + " (" +
                InterruptionContract.InterruptionsColumns.HOUR + cs +
                InterruptionContract.InterruptionsColumns.MINUTES + cs +
                InterruptionContract.InterruptionsColumns.DAYS_OF_WEEK + cs +
                InterruptionContract.InterruptionsColumns.ENABLED + cs +
                InterruptionContract.InterruptionsColumns.VIBRATE + cs +
                InterruptionContract.InterruptionsColumns.LABEL + cs +
                InterruptionContract.InterruptionsColumns.NOTIFICATION + cs +
                InterruptionContract.InterruptionsColumns.DELETE_AFTER_USE + ") VALUES ";
        db.execSQL(insertMe + DEFAULT_INTERRUPTION_1);
    }
}