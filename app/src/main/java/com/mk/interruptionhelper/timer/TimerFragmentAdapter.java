package com.mk.interruptionhelper.timer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.support.v4.view.PagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by MK on 2015/7/26.
 */
public class TimerFragmentAdapter extends FragmentStatePagerAdapter2 {

    private final ArrayList<TimerObj> mTimerList = new ArrayList<TimerObj>();
    private final SharedPreferences mSharedPrefs;

    public TimerFragmentAdapter(FragmentManager fm, SharedPreferences sharedPreferences) {
        super(fm);
        mSharedPrefs = sharedPreferences;
    }

    @Override
    public int getItemPosition(Object object) {
        // Force return NONE so that the adapter always assumes the item position has changed
        return PagerAdapter.POSITION_NONE;
    }

    @Override
    public int getCount() {
        return mTimerList.size();
    }

    @Override
    public Fragment getItem(int position) {
        //return TimerItemFragment.newInstance(mTimerList.get(position));
        return null;
    }

    public void addTimer(TimerObj timer) {
        // Newly created timer should always show on the top of the list
        mTimerList.add(0, timer);
        notifyDataSetChanged();
    }

    public TimerObj getTimerAt(int position) {
        return mTimerList.get(position);
    }

    public void saveTimersToSharedPrefs() {
        TimerObj.putTimersInSharedPrefs(mSharedPrefs, mTimerList);
    }

    public void populateTimersFromPref() {
        mTimerList.clear();
        TimerObj.getTimersFromSharedPrefs(mSharedPrefs, mTimerList);
        Collections.sort(mTimerList, new Comparator<TimerObj>() {
            @Override
            public int compare(TimerObj o1, TimerObj o2) {
                return (o2.mTimerId < o1.mTimerId) ? -1 : 1;
            }
        });

        notifyDataSetChanged();
    }

    public void deleteTimer(int id) {
        for (int i = 0; i < mTimerList.size(); i++) {
            TimerObj timer = mTimerList.get(i);
            if (timer.mTimerId == id) {
//                if (timer.mView != null) {
//                    timer.mView.stop();
//                }
                timer.deleteFromSharedPref(mSharedPrefs);
                mTimerList.remove(i);
                break;
            }
        }

        notifyDataSetChanged();
        return;
    }
}
