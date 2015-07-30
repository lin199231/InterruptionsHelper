package com.mk.interruptionhelper;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
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
import android.support.v13.app.FragmentTabHost;
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
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

import com.mk.interruptionhelper.R;
import com.mk.interruptionhelper.provider.Interruption;
import com.mk.interruptionhelper.timer.TimerObj;
import com.mk.interruptionhelper.util.Utils;

public class InterruptionHelper extends FragmentActivity implements LabelDialogFragment.TimerLabelDialogHandler,
        LabelDialogFragment.AlarmLabelDialogHandler {

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
    private ClockFragment mClockFragment;
    private Menu mMenu;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
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
        if (mTabsAdapter == null) {
            mViewPager = (ViewPager) findViewById(R.id.interruption_helper_pager);
            // Keep all four tabs to minimize jank.
            mViewPager.setOffscreenPageLimit(3);
            mTabHost = (FragmentTabHost)findViewById(R.id.tab_host);
            mTabHost.setup(this, getFragmentManager(), R.id.interruption_helper_pager);

            mTabsAdapter = new TabsAdapter(this, mViewPager, mTabHost);

            createTabs(mSelectedTab);
        }

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //getSelectedFragment().onFabClick(view);
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

    private InterruptionHelperFragment getSelectedFragment() {
//        switch (mTabHost.getCurrentTab()){
//            case INTERRUPTION_TAB_INDEX:
//                return
//            case CLOCK_TAB_INDEX:
//                return mClockFragment;
//            default:
//                return mClockFragment;
//        }
        return (InterruptionHelperFragment) mTabsAdapter.getItem(getRtlPosition(mSelectedTab));
    }

    private void createTabs(int selectedIndex) {
        mActionBar = getActionBar();
        mActionBar.hide();


        if (mTabHost != null) {
            mInterruptionTab = mTabHost.newTabSpec("Interruption").setIndicator(null, getResources().getDrawable(R.drawable.alarm_tab, null));
            mClockTab = mTabHost.newTabSpec("Clock").setIndicator(null,getResources().getDrawable(R.drawable.clock_tab, null));

            mTabsAdapter.addTab(mInterruptionTab, InterruptionFragment.class, INTERRUPTION_TAB_INDEX);
            mTabsAdapter.addTab(mClockTab, ClockFragment.class, CLOCK_TAB_INDEX);
            //mTabHost.addTab(mClockTab,ClockFragment.class,null);
            //mTabHost.addTab(mInterruptionTab,ClockFragment.class,null);




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
            mTabHost.setCurrentTab(selectedIndex);
            mTabsAdapter.notifySelectedPage(selectedIndex);
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        //setContentView(R.layout.interruptions_helper);
        mIsFirstLaunch = (icicle == null);
        getWindow().setBackgroundDrawable(null);

        mIsFirstLaunch = true;
        mSelectedTab = CLOCK_TAB_INDEX;
        if (icicle != null) {
            mSelectedTab = icicle.getInt(KEY_SELECTED_TAB, CLOCK_TAB_INDEX);
            mLastHourColor = icicle.getInt(KEY_LAST_HOUR_COLOR, UNKNOWN_COLOR_ID);
            if (mLastHourColor != UNKNOWN_COLOR_ID) {
                getWindow().getDecorView().setBackgroundColor(mLastHourColor);
            }
        }

        // Timer receiver may ask the app to go to the timer fragment if a timer expired
        Intent i = getIntent();
        if (i != null) {
            int tab = i.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
            if (tab != -1) {
                mSelectedTab = tab;
            }
        }
        initViews();
//        setHomeTimeZone();

        // We need to update the system next alarm time on app startup because the
        // user might have clear our data.
//        AlarmStateManager.updateNextAlarm(this);
//        ExtensionsFactory.init(getAssets());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu(menu);
        return true;
    }

    private void updateMenu(Menu menu) {
        // Hide "help" if we don't have a URI for it.
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }

        // Hide "lights out" for timer.
        MenuItem nightMode = menu.findItem(R.id.menu_item_night_mode);
        if (mTabHost.getCurrentTab() == INTERRUPTION_TAB_INDEX) {
            nightMode.setVisible(false);
        } else if (mTabHost.getCurrentTab() == CLOCK_TAB_INDEX) {
//            nightMode.setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (processMenuClick(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean processMenuClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivity(new Intent(InterruptionHelper.this, SettingsActivity.class));
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        // No activity found to match the intent - ignore
                    }
                }
                return true;
            case R.id.menu_item_night_mode:
                startActivity(new Intent(InterruptionHelper.this, ScreensaverActivity.class));
            default:
                break;
        }
        return true;
    }

    public void registerPageChangedListener(InterruptionHelperFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.registerPageChangedListener(frag);
        }
    }

    public void unregisterPageChangedListener(InterruptionHelperFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.unregisterPageChangedListener(frag);
        }
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

    /**
     * Adapter for wrapping together the ActionBar's tab with the ViewPager
     */
    private class TabsAdapter extends FragmentPagerAdapter
        implements FragmentTabHost.OnTabChangeListener, ViewPager.OnPageChangeListener{

        private static final String KEY_TAB_POSITION = "tab_position";

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, int position) {
                clss = _class;
                args = new Bundle();
                args.putInt(KEY_TAB_POSITION, position);
            }

            public int getPosition() {
                return args.getInt(KEY_TAB_POSITION, 0);
            }
        }

        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
        FragmentTabHost mHost;
        ActionBar mMainActionBar;
        Context mContext;
        ViewPager mPager;
        // Used for doing callbacks to fragments.
        HashSet<String> mFragmentTags = new HashSet<String>();

        public TabsAdapter(Activity activity, ViewPager pager, FragmentTabHost tabHost) {
            super(activity.getFragmentManager());
            mContext = activity;
            mMainActionBar = activity.getActionBar();
            mPager = pager;
            mHost = tabHost;
            mPager.setAdapter(this);
            mPager.addOnPageChangeListener(this);
            mHost.setOnTabChangedListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            // Because this public method is called outside many times,
            // check if it exits first before creating a new one.
            final String name = makeFragmentName(R.id.interruption_helper_pager, position);
            Fragment fragment = getFragmentManager().findFragmentByTag(name);
            if (fragment == null) {
                //TabInfo info = mTabs.get(getRtlPosition(position));
                fragment = Fragment.instantiate(mContext, getFragmentName(position));
//                if (fragment instanceof TimerFragment) {
//                    ((TimerFragment) fragment).setFabAppearance();
//                    ((TimerFragment) fragment).setLeftRightButtonAppearance();
//                }
            }
            return fragment;
        }

        /**
         * Copied from:
         * android/frameworks/support/v13/java/android/support/v13/app/FragmentPagerAdapter.java#94
         * Create unique name for the fragment so fragment manager knows it exist.
         */
        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }

        private String getFragmentName(int index){
            if (isRtl()) {
                switch (index) {
//                    case TIMER_TAB_INDEX:
//                        return null;
                    case RTL_CLOCK_TAB_INDEX:
                        return ClockFragment.class.getName();
//                    case INTERRUPTION_TAB_INDEX:
//                        return RTL_INTERRUPTION_TAB_INDEX;
                    default:
                        return null;
                }
            }else {
                switch (index) {
//                    case TIMER_TAB_INDEX:
//                        return null;
                    case CLOCK_TAB_INDEX:
                        return ClockFragment.class.getName();
//                    case INTERRUPTION_TAB_INDEX:
//                        return RTL_INTERRUPTION_TAB_INDEX;
                    default:
                        return null;
                }
            }
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        public void addTab(TabSpec tab, Class<?> clss, int position) {
            TabInfo info = new TabInfo(clss, position);
            mHost.addTab(tab, clss , info.args);
            notifyDataSetChanged();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Do nothing
        }

        @Override
        public void onPageSelected(int position) {
            // Set the page before doing the menu so that onCreateOptionsMenu knows what page it is.
            mHost.setCurrentTab(getRtlPosition(position));
            notifyPageChanged(position);

            // Only show the overflow menu for alarm and world clock.
            if (mMenu != null) {
                // Make sure the menu's been initialized.
                if (position == INTERRUPTION_TAB_INDEX || position == CLOCK_TAB_INDEX) {
                    mMenu.setGroupVisible(R.id.menu_items, true);
                    onCreateOptionsMenu(mMenu);
                } else {
                    mMenu.setGroupVisible(R.id.menu_items, false);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing
        }

        @Override
        public void onTabChanged(String tabId){
            //Toast.makeText(getApplicationContext(), "TabId=" + tabId, Toast.LENGTH_LONG).show();
        }

        private boolean isClockTab(int rtlSafePosition) {
            final int clockTabIndex = isRtl() ? RTL_CLOCK_TAB_INDEX : CLOCK_TAB_INDEX;
            return rtlSafePosition == clockTabIndex;
        }

        public void notifySelectedPage(int page) {
            notifyPageChanged(page);
        }

        private void notifyPageChanged(int newPage) {
            for (String tag : mFragmentTags) {
                final FragmentManager fm = getFragmentManager();
                InterruptionHelperFragment f = (InterruptionHelperFragment) fm.findFragmentByTag(tag);
                if (f != null) {
                    f.onPageChanged(newPage);
                }
            }
        }

        public void registerPageChangedListener(InterruptionHelperFragment frag) {
            String tag = frag.getTag();
            if (mFragmentTags.contains(tag)) {
                Log.wtf(LOG_TAG, "Trying to add an existing fragment " + tag);
            } else {
                mFragmentTags.add(frag.getTag());
            }
            // Since registering a listener by the fragment is done sometimes after the page
            // was already changed, make sure the fragment gets the current page
            frag.onPageChanged(mHost.getCurrentTab());
        }

        public void unregisterPageChangedListener(InterruptionHelperFragment frag) {
            mFragmentTags.remove(frag.getTag());
        }
    }

    /**
     * Called by the LabelDialogFormat class after the dialog is finished. *
     */
    @Override
    public void onDialogLabelSet(TimerObj timer, String label, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
//        if (frag instanceof TimerFragment) {
//            ((TimerFragment) frag).setLabel(timer, label);
//        }
    }

    /**
     * Called by the LabelDialogFormat class after the dialog is finished. *
     */
    @Override
    public void onDialogLabelSet(Interruption interruption, String label, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof InterruptionFragment) {
            ((InterruptionFragment) frag).setLabel(interruption, label);
        }
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
