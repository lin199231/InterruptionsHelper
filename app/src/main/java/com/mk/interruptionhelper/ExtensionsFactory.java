package com.mk.interruptionhelper;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.mk.interruptionhelper.provider.Interruption;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by dhdev_000 on 2015/7/30.
 */
public class ExtensionsFactory {

    private static String TAG = "ExtensionsFactory";
    // Config filename for mappings of various class names to their custom
    // implementations.
    private static String EXTENSIONS_PROPERTIES = "interruptionhelper_extensions.properties";
    private static String INTERRUPTIONHELPER_EXTENSIONS_KEY = "InterruptionHelperExtensions";
    private static Properties sProperties = new Properties();
    private static InterruptionHelperExtensions sInterruptionHelperExtensions = null;

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

    public static InterruptionHelperExtensions getInterruptionHelperExtensions() {
        if ((sInterruptionHelperExtensions != null)) {
            return sInterruptionHelperExtensions;
        }

        String className = sProperties.getProperty(INTERRUPTIONHELPER_EXTENSIONS_KEY);
        if (className != null) {
            sInterruptionHelperExtensions = createInstance(className);
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, INTERRUPTIONHELPER_EXTENSIONS_KEY + " not found in properties file.");
            }
        }

        if (sInterruptionHelperExtensions == null) {
            sInterruptionHelperExtensions = new InterruptionHelperExtensions() {
                @Override
                public void addInterruption(Context context, Interruption newInterruption) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Add interruption: Empty inline implementation called.");
                    }
                }

                @Override
                public void deleteInterruption(Context context, long alarmId) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Delete alarm: Empty inline implementation called.");
                    }
                }
            };
        }
        return sInterruptionHelperExtensions;
    }
}
