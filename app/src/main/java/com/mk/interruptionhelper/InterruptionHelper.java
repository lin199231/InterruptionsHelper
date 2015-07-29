package com.mk.interruptionhelper;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TabActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

import com.mk.interruptionhelper.R;

public class InterruptionHelper extends FragmentActivity {

    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "InterruptionHelper";
    // Interruption action for midnight (so we can update the date display).
    private static final String KEY_SELECTED_TAB = "selected_tab";
    private static final String KEY_LAST_HOUR_COLOR = "last_hour_color";
    // Check whether to change background every minute
    private static final long BACKGROUND_COLOR_CHECK_DELAY_MILLIS = DateUtils.MINUTE_IN_MILLIS;
    private static final int BACKGROUND_COLOR_INITIAL_ANIMATION_DURATION_MILLIS = 3000;
    // The depth of fab, use it to create shadow
    private static final float FAB_DEPTH = 20f;
    private static final int UNKNOWN_COLOR_ID = 0;

    private boolean mIsFirstLaunch = true;
    private ActionBar mActionBar;
    private FragmentTabHost mTabHost;
    private TabSpec mInterruptionTab;
    private TabSpec mClockTab;
    private TabSpec mTimerTab;
    private  ClockFragment mClockFragment;
    private Menu mMenu;
    private ViewPager mViewPager;
    private Handler mHander;
    private ImageButton mFab;
    private ImageButton mLeftButton;
    private ImageButton mRightButton;
    private int mSelectedTab;
    private int mLastHourColor = UNKNOWN_COLOR_ID;
    private final Runnable mBackgroundColorChanger = new Runnable() {
        @Override
        public void run() {
            setBackgroundColor();
            mHander.postDelayed(this, BACKGROUND_COLOR_CHECK_DELAY_MILLIS);
        }
    };

    public static final int INTERRUPTION_TAB_INDEX = 0;
    public static final int CLOCK_TAB_INDEX = 1;
    public static final int TIMER_TAB_INDEX = 2;
    // Tabs indices are switched for right-to-left since there is no
    // native support for RTL in the ViewPager.
    public static final int RTL_INTERRUPTION_TAB_INDEX = 2;
    public static final int RTL_CLOCK_TAB_INDEX = 1;
    public static final int RTL_TIMER_TAB_INDEX = 0;
    public static final String SELECT_TAB_INTENT_EXTRA = "interruptionhelper.select.tab";

    // TODO(rachelzhang): adding a broadcast receiver to adjust color when the timezone/time
    // changes in the background.

    @Override
    protected void onStart() {
        super.onStart();
        if (mHander == null) {
            mHander = new Handler();
        }
        mHander.postDelayed(mBackgroundColorChanger, BACKGROUND_COLOR_CHECK_DELAY_MILLIS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHander.removeCallbacks(mBackgroundColorChanger);
    }

    private static final ViewOutlineProvider OVAL_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    };

    private void initViews() {
        setContentView(R.layout.interruptions_helper);
        mFab = (ImageButton) findViewById(R.id.fab);
        mFab.setOutlineProvider(OVAL_OUTLINE_PROVIDER);
        mFab.setTranslationZ(FAB_DEPTH);
        mLeftButton = (ImageButton) findViewById(R.id.left_button);
        mRightButton = (ImageButton) findViewById(R.id.right_button);
        if (mTabHost == null) {
            mViewPager = (ViewPager) findViewById(R.id.interruption_helper_pager);
            // Keep all four tabs to minimize jank.
            mViewPager.setOffscreenPageLimit(3);
//            mTabsAdapter = new TabsAdapter(this, mViewPager);
//            createTabs(mSelectedTab);

            createTabs(mSelectedTab);
        }

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //getSelectedFragment().onFabClick(view);
                FabClickTest();
            }
        });
        mLeftButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedFragment().onLeftButtonClick(view);
            }
        });
        mRightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedFragment().onRightButtonClick(view);
            }
        });

        mTabHost.setCurrentTab(mSelectedTab);
    }

    private void FabClickTest(){
        Toast.makeText(this,"Fab clicked",Toast.LENGTH_LONG).show();
    }
    private InterruptionHelperFragment getSelectedFragment() {
        switch (mTabHost.getCurrentTab()){
//            case INTERRUPTION_TAB_INDEX:
//                return
            case CLOCK_TAB_INDEX:
                return mClockFragment;
            default:
                return mClockFragment;
        }
    }

    private void createTabs(int selectedIndex) {
        mActionBar = getActionBar();
        mActionBar.hide();

        mTabHost = (FragmentTabHost)findViewById(R.id.tab_host);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.interruption_helper_pager);
        if (mTabHost != null) {
            mClockTab = mTabHost.newTabSpec("Clock1").setIndicator("Clock1");
            mInterruptionTab = mTabHost.newTabSpec("Clock2").setIndicator("Clock2");
            mTabHost.addTab(mClockTab,ClockFragment.class,null);
            mTabHost.addTab(mInterruptionTab,ClockFragment.class,null);



//            mClockTab = mActionBar.newTab();
//            mClockTab.setIcon(R.drawable.clock_tab);
//            mClockTab.setContentDescription(R.string.menu_clock);
//            mTabsAdapter.addTab(mClockTab, ClockFragment.class, CLOCK_TAB_INDEX);
//
//            mTimerTab = mActionBar.newTab();
//            mTimerTab.setIcon(R.drawable.timer_tab);
//            mTimerTab.setContentDescription(R.string.menu_timer);
//            mTabsAdapter.addTab(mTimerTab, TimerFragment.class, TIMER_TAB_INDEX);
//
//            mStopwatchTab = mActionBar.newTab();
//            mStopwatchTab.setIcon(R.drawable.stopwatch_tab);
//            mStopwatchTab.setContentDescription(R.string.menu_stopwatch);
//            mTabsAdapter.addTab(mStopwatchTab, StopwatchFragment.class, STOPWATCH_TAB_INDEX);
//
//            mActionBar.setSelectedNavigationItem(selectedIndex);
//            mTabsAdapter.notifySelectedPage(selectedIndex);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.interruptions_helper);
        initViews();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }
        */
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
        /*
        final int currHourColor = Utils.getCurrentHourColor();
        if (mLastHourColor != currHourColor) {
            final ObjectAnimator animator = ObjectAnimator.ofInt(getWindow().getDecorView(),
                    "backgroundColor", mLastHourColor, currHourColor);
            animator.setDuration(duration);
            animator.setEvaluator(new ArgbEvaluator());
            animator.start();
            mLastHourColor = currHourColor;
        }
         */
    }

    public int getSelectedTab() {
        return mSelectedTab;
    }

    private boolean isRtl() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_RTL;
    }

    private int getRtlPosition(int position) {
        if (isRtl()) {
            switch (position) {
                case TIMER_TAB_INDEX:
                    return RTL_TIMER_TAB_INDEX;
                case CLOCK_TAB_INDEX:
                    return RTL_CLOCK_TAB_INDEX;
                case INTERRUPTION_TAB_INDEX:
                    return RTL_INTERRUPTION_TAB_INDEX;
                default:
                    break;
            }
        }
        return position;
    }
    public ImageButton getFab() {
        return mFab;
    }

    public ImageButton getLeftButton() {
        return mLeftButton;
    }

    public ImageButton getRightButton() {
        return mRightButton;
    }
}
