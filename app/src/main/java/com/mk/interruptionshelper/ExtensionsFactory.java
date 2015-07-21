package com.mk.interruptionshelper;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.mk.interruptionshelper.provider.InterruptionsAlarm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by dhdev_000 on 2015/7/21.
 */
public class ExtensionsFactory {

    private static String TAG = "ExtensionsFactory";
    // Config filename for mappings of various class names to their custom
    // implementations.
    private static String EXTENSIONS_PROPERTIES = "interruptionshelper_extensions.properties";
    private static String DESKCLOCKEXTENSIONS_KEY = "InterruptionsHelperExtensions";
    private static Properties sProperties = new Properties();
    private static InterruptionsHelperExtensions sInterruptionsHelperExtensions = null;

    public static void init(AssetManager assetManager) {
        try {
            InputStream fileStream = assetManager.open(EXTENSIONS_PROPERTIES);
            sProperties.load(fileStream);
            fileStream.close();
        } catch (FileNotFoundException e) {
            // No custom extensions. Ignore.
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No custom extensions.");
            }
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, e.toString());
            }
        }
    }

    private static <T> T createInstance(String className) {
        try {
            Class<?> c = Class.forName(className);
            return (T) c.newInstance();
        } catch (ClassNotFoundException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, className + ": unable to create instance.", e);
            }
        } catch (IllegalAccessException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, className + ": unable to create instance.", e);
            }
        } catch (InstantiationException e) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, className + ": unable to create instance.", e);
            }
        }
        return null;
    }

    public static InterruptionsHelperExtensions getInterruptionsHelperExtensions() {
        if ((sInterruptionsHelperExtensions != null)) {
            return sInterruptionsHelperExtensions;
        }

        String className = sProperties.getProperty(DESKCLOCKEXTENSIONS_KEY);
        if (className != null) {
            sInterruptionsHelperExtensions = createInstance(className);
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, DESKCLOCKEXTENSIONS_KEY + " not found in properties file.");
            }
        }

        if (sInterruptionsHelperExtensions == null) {
            sInterruptionsHelperExtensions = new sInterruptionsHelperExtensions() {
                @Override
                public void addAlarm(Context context, InterruptionsAlarm newAlarm) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Add alarm: Empty inline implementation called.");
                    }
                }

                @Override
                public void deleteAlarm(Context context, long alarmId) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Delete alarm: Empty inline implementation called.");
                    }
                }
            };
        }
        return sInterruptionsHelperExtensions;
    }
}
