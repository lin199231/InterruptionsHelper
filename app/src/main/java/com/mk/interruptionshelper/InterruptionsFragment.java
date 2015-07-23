package com.mk.interruptionshelper;

import android.content.Loader;
import android.os.Bundle;
import android.transition.Transition;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.mk.interruptionshelper.provider.InterruptionsAlarm;

/**
 * Created by dhdev_000 on 2015/7/21.
 */
public class InterruptionsFragment extends InterruptionsHelperFragment {
    private static final float EXPAND_DECELERATION = 1f;
    private static final float COLLAPSE_DECELERATION = 0.7f;

    private static final int ANIMATION_DURATION = 300;
    private static final int EXPAND_DURATION = 300;
    private static final int COLLAPSE_DURATION = 250;

    private static final int ROTATE_180_DEGREE = 180;
    private static final float ALARM_ELEVATION = 8f;
    private static final float TINTED_LEVEL = 0.09f;

    private static final String KEY_EXPANDED_ID = "expandedId";
    private static final String KEY_REPEAT_CHECKED_IDS = "repeatCheckedIds";
    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String KEY_SELECTED_ALARMS = "selectedAlarms";
    private static final String KEY_DELETED_ALARM = "deletedAlarm";
    private static final String KEY_UNDO_SHOWING = "undoShowing";
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private static final String KEY_SELECTED_ALARM = "selectedAlarm";
    private static final InterruptionsHelperExtensions sDeskClockExtensions = ExtensionsFactory.getInterruptionsHelperExtensions();

    private static final int REQUEST_CODE_RINGTONE = 1;
    private static final long INVALID_ID = -1;

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private FrameLayout mMainLayout;
    private ListView mAlarmsList;
    //private AlarmItemAdapter mAdapter;
    private View mEmptyView;
    private View mFooterView;

    private Bundle mRingtoneTitleCache; // Key: ringtone uri, value: ringtone title
    //private ActionableToastBar mUndoBar;
    private View mUndoFrame;

    private InterruptionsAlarm mSelectedAlarm;
    private long mScrollToAlarmId = INVALID_ID;

    private Loader mCursorLoader = null;

    // Saved states for undo
    private InterruptionsAlarm mDeletedAlarm;
    private InterruptionsAlarm mAddedAlarm;
    private boolean mUndoShowing;

    private Interpolator mExpandInterpolator;
    private Interpolator mCollapseInterpolator;

    private Transition mAddRemoveTransition;
    private Transition mRepeatTransition;
    private Transition mEmptyViewTransition;

    public InterruptionsFragment() {
        // Basic provider required by Fragment.java
    }

}
