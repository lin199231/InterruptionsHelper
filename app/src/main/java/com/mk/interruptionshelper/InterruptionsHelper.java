package com.mk.interruptionshelper;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.text.format.DateUtils;

public class InterruptionsHelper extends Activity {

    private static final long BACKGROUND_COLOR_CHECK_DELAY_MILLIS = DateUtils.MINUTE_IN_MILLIS;
    private static final int BACKGROUND_COLOR_INITIAL_ANIMATION_DURATION_MILLIS = 3000;

    private static final int UNKNOWN_COLOR_ID = 0;

    private Handler mHander;
    private int mLastHourColor = UNKNOWN_COLOR_ID;
    private final Runnable mBackgroundColorChanger = new Runnable() {
        @Override
        public void run() {
            setBackgroundColor();
            mHander.postDelayed(this, BACKGROUND_COLOR_CHECK_DELAY_MILLIS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interruptions_helper);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_interruptions_helper, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setBackgroundColor() {
        final int duration;
        if (mLastHourColor == UNKNOWN_COLOR_ID) {
            mLastHourColor = getResources().getColor(R.color.default_background);
            duration = BACKGROUND_COLOR_INITIAL_ANIMATION_DURATION_MILLIS;
        } else {
            duration = getResources().getInteger(android.R.integer.config_longAnimTime);
        }
        final int currHourColor = Utils.getCurrentHourColor();
        if (mLastHourColor != currHourColor) {
            final ObjectAnimator animator = ObjectAnimator.ofInt(getWindow().getDecorView(),
                    "backgroundColor", mLastHourColor, currHourColor);
            animator.setDuration(duration);
            animator.setEvaluator(new ArgbEvaluator());
            animator.start();
            mLastHourColor = currHourColor;
        }
    }
}
