package com.mk.interruptionshelper.worldclock;

/**
 * Created by MK on 2015/7/12.
 */


import android.content.SharedPreferences;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;


public class Cities {

    public static final String WORLDCLOCK_UPDATE_INTENT = "com.android.deskclock.worldclock.update";
    private static final String NUMBER_OF_CITIES = "number_of_cities";

    public static void saveCitiesToSharedPrefs(
            SharedPreferences prefs, HashMap<String, CityObj> cities) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(NUMBER_OF_CITIES, cities.size());
        Collection<CityObj> col = cities.values();
        Iterator<CityObj> i = col.iterator();
        int count = 0;
        while (i.hasNext()) {
            CityObj c = i.next();
            c.saveCityToSharedPrefs(editor, count);
            count++;
        }
        editor.apply();
    }

    public static  HashMap<String, CityObj> readCitiesFromSharedPrefs(SharedPreferences prefs) {
        int size = prefs.getInt(NUMBER_OF_CITIES, -1);
        HashMap<String, CityObj> c = new HashMap<String, CityObj>();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                CityObj o = new CityObj(prefs, i);
                if (o.mCityName != null && o.mTimeZone != null) {
                    c.put(o.mCityId, o);
                }
            }
        }
        return c;
    }

    private static void dumpCities(SharedPreferences prefs, String title) {
        int size = prefs.getInt(NUMBER_OF_CITIES, -1);
        Log.d("Cities", "Selected Cities List " + title);
        Log.d("Cities", "Number of cities " + size);
        HashMap<String, CityObj> c = new HashMap<String, CityObj>();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                CityObj o = new CityObj(prefs, i);
                if (o.mCityName != null && o.mTimeZone != null) {
                    Log.d("Cities", "Name " + o.mCityName + " tz " + o.mTimeZone);
                }
            }
        }
    }
}
