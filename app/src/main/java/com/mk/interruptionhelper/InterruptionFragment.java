package com.mk.interruptionhelper;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.mk.interruptionhelper.interruption.InterruptionStateManager;
import com.mk.interruptionhelper.provider.DaysOfWeek;
import com.mk.interruptionhelper.provider.Interruption;
import com.mk.interruptionhelper.provider.InterruptionInstance;
import com.mk.interruptionhelper.util.InterruptionUtils;
import com.mk.interruptionhelper.util.LogUtils;
import com.mk.interruptionhelper.util.Utils;
import com.mk.interruptionhelper.widget.ActionableToastBar;
import com.mk.interruptionhelper.widget.TextTime;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

/**
 * Created by dhdev_000 on 2015/7/30.
 */
public class InterruptionFragment extends InterruptionHelperFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnTimeSetListener, View.OnTouchListener {
    private static final float EXPAND_DECELERATION = 1f;
    private static final float COLLAPSE_DECELERATION = 0.7f;

    private static final int ANIMATION_DURATION = 300;
    private static final int EXPAND_DURATION = 300;
    private static final int COLLAPSE_DURATION = 250;

    private static final int ROTATE_180_DEGREE = 180;
    private static final float INTERRUPTION_ELEVATION = 8f;
    private static final float TINTED_LEVEL = 0.09f;

    private static final String KEY_EXPANDED_ID = "expandedId";
    private static final String KEY_REPEAT_CHECKED_IDS = "repeatCheckedIds";
    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String KEY_SELECTED_INTERRUPTIONS = "selectedInterruptions";
    private static final String KEY_DELETED_INTERRUPTION = "deletedInterruption";
    private static final String KEY_UNDO_SHOWING = "undoShowing";
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private static final String KEY_SELECTED_INTERRUPTION = "selectedInterruption";
    private static final InterruptionHelperExtensions sInterruptionHelperExtensions = ExtensionsFactory
            .getInterruptionHelperExtensions();

    private static final int REQUEST_CODE_RINGTONE = 1;
    private static final long INVALID_ID = -1;

    // This extra is used when receiving an intent to create an interruption, but no interruption details
    // have been passed in, so the interruption page should start the process of creating a new interruption.
    public static final String INTERRUPTION_CREATE_NEW_INTENT_EXTRA = "interruptionhelper.create.new";

    // This extra is used when receiving an intent to scroll to specific interruption. If interruption
    // can not be found, and toast message will pop up that the interruption has be deleted.
    public static final String SCROLL_TO_INTERRUPTION_INTENT_EXTRA = "interruptionhelper.scroll.to.interruption";

    private FrameLayout mMainLayout;
    private ListView mInterruptionsList;
    private InterruptionItemAdapter mAdapter;
    private View mEmptyView;
    private View mFooterView;

    private Bundle mRingtoneTitleCache; // Key: ringtone uri, value: ringtone title
    private ActionableToastBar mUndoBar;
    private View mUndoFrame;

    private Interruption mSelectedInterruption;
    private long mScrollToInterruptionId = INVALID_ID;

    private Loader mCursorLoader = null;

    // Saved states for undo
    private Interruption mDeletedInterruption;
    private Interruption mAddedInterruption;
    private boolean mUndoShowing;

    private Interpolator mExpandInterpolator;
    private Interpolator mCollapseInterpolator;

    private Transition mAddRemoveTransition;
    private Transition mRepeatTransition;
    private Transition mEmptyViewTransition;

    public InterruptionFragment() {
        // Basic provider required by Fragment.java
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mCursorLoader = getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.interruption_clock, container, false);

        long expandedId = INVALID_ID;
        long[] repeatCheckedIds = null;
        long[] selectedInterruptions = null;
        Bundle previousDayMap = null;
        if (savedState != null) {
            expandedId = savedState.getLong(KEY_EXPANDED_ID);
            repeatCheckedIds = savedState.getLongArray(KEY_REPEAT_CHECKED_IDS);
            mRingtoneTitleCache = savedState.getBundle(KEY_RINGTONE_TITLE_CACHE);
            mDeletedInterruption = savedState.getParcelable(KEY_DELETED_INTERRUPTION);
            mUndoShowing = savedState.getBoolean(KEY_UNDO_SHOWING);
            selectedInterruptions = savedState.getLongArray(KEY_SELECTED_INTERRUPTIONS);
            previousDayMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
            mSelectedInterruption = savedState.getParcelable(KEY_SELECTED_INTERRUPTION);
        }

        mExpandInterpolator = new DecelerateInterpolator(EXPAND_DECELERATION);
        mCollapseInterpolator = new DecelerateInterpolator(COLLAPSE_DECELERATION);

        mAddRemoveTransition = new AutoTransition();
        mAddRemoveTransition.setDuration(ANIMATION_DURATION);

        mRepeatTransition = new AutoTransition();
        mRepeatTransition.setDuration(ANIMATION_DURATION / 2);
        mRepeatTransition.setInterpolator(new AccelerateDecelerateInterpolator());

        mEmptyViewTransition = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
                .addTransition(new Fade(Fade.OUT))
                .addTransition(new Fade(Fade.IN))
                .setDuration(ANIMATION_DURATION);

        boolean isLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        View menuButton = v.findViewById(R.id.menu_button);
        if (menuButton != null) {
            if (isLandscape) {
                menuButton.setVisibility(View.GONE);
            } else {
                menuButton.setVisibility(View.VISIBLE);
                setupFakeOverflowMenuButton(menuButton);
            }
        }

        mEmptyView = v.findViewById(R.id.alarms_empty_view);

        mMainLayout = (FrameLayout) v.findViewById(R.id.main);
        mInterruptionsList = (ListView) v.findViewById(R.id.alarms_list);

        mUndoBar = (ActionableToastBar) v.findViewById(R.id.undo_bar);
        mUndoFrame = v.findViewById(R.id.undo_frame);
        mUndoFrame.setOnTouchListener(this);

        mFooterView = v.findViewById(R.id.alarms_footer_view);
        mFooterView.setOnTouchListener(this);

        mAdapter = new InterruptionItemAdapter(getActivity(),
                expandedId, repeatCheckedIds, selectedInterruptions, previousDayMap, mInterruptionsList);
        mAdapter.registerDataSetObserver(new DataSetObserver() {

            private int prevAdapterCount = -1;

            @Override
            public void onChanged() {

                final int count = mAdapter.getCount();
                if (mDeletedInterruption != null && prevAdapterCount > count) {
                    showUndoBar();
                }

                if ((count == 0 && prevAdapterCount > 0) ||  /* should fade in */
                        (count > 0 && prevAdapterCount == 0) /* should fade out */) {
                    TransitionManager.beginDelayedTransition(mMainLayout, mEmptyViewTransition);
                }
                mEmptyView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

                // Cache this adapter's count for when the adapter changes.
                prevAdapterCount = count;
                super.onChanged();
            }
        });

        if (mRingtoneTitleCache == null) {
            mRingtoneTitleCache = new Bundle();
        }

        mInterruptionsList.setAdapter(mAdapter);
        mInterruptionsList.setVerticalScrollBarEnabled(true);
        mInterruptionsList.setOnCreateContextMenuListener(this);

        if (mUndoShowing) {
            showUndoBar();
        }
        return v;
    }

    private void setUndoBarRightMargin(int margin) {
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) mUndoBar.getLayoutParams();
        ((FrameLayout.LayoutParams) mUndoBar.getLayoutParams())
                .setMargins(params.leftMargin, params.topMargin, margin, params.bottomMargin);
        mUndoBar.requestLayout();
    }

    @Override
    public void onResume() {
        super.onResume();

        final InterruptionHelper activity = (InterruptionHelper) getActivity();
        if (activity.getSelectedTab() == InterruptionHelper.INTERRUPTION_TAB_INDEX) {
            setFabAppearance();
            setLeftRightButtonAppearance();
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        // Check if another app asked us to create a blank new interruption.
        final Intent intent = getActivity().getIntent();
        if (intent.hasExtra(INTERRUPTION_CREATE_NEW_INTENT_EXTRA)) {
            if (intent.getBooleanExtra(INTERRUPTION_CREATE_NEW_INTENT_EXTRA, false)) {
                // An external app asked us to create a blank interruption.
                startCreatingInterruption();
            }

            // Remove the CREATE_NEW extra now that we've processed it.
            intent.removeExtra(INTERRUPTION_CREATE_NEW_INTENT_EXTRA);
        } else if (intent.hasExtra(SCROLL_TO_INTERRUPTION_INTENT_EXTRA)) {
            long interruptionId = intent.getLongExtra(SCROLL_TO_INTERRUPTION_INTENT_EXTRA, Interruption.INVALID_ID);
            if (interruptionId != Interruption.INVALID_ID) {
                mScrollToInterruptionId = interruptionId;
                if (mCursorLoader != null && mCursorLoader.isStarted()) {
                    // We need to force a reload here to make sure we have the latest view
                    // of the data to scroll to.
                    mCursorLoader.forceLoad();
                }
            }

            // Remove the SCROLL_TO_INTERRUPTION extra now that we've processed it.
            intent.removeExtra(SCROLL_TO_INTERRUPTION_INTENT_EXTRA);
        }
    }

    private void hideUndoBar(boolean animate, MotionEvent event) {
        if (mUndoBar != null) {
            mUndoFrame.setVisibility(View.GONE);
            if (event != null && mUndoBar.isEventInToastBar(event)) {
                // Avoid touches inside the undo bar.
                return;
            }
            mUndoBar.hide(animate);
        }
        mDeletedInterruption = null;
        mUndoShowing = false;
    }

    private void showUndoBar() {
        final Interruption deletedInterruption = mDeletedInterruption;
        mUndoFrame.setVisibility(View.VISIBLE);
        mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
            @Override
            public void onActionClicked() {
                mAddedInterruption = deletedInterruption;
                mDeletedInterruption = null;
                mUndoShowing = false;

                asyncAddInterruption(deletedInterruption);
            }
        }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_EXPANDED_ID, mAdapter.getExpandedId());
        outState.putLongArray(KEY_REPEAT_CHECKED_IDS, mAdapter.getRepeatArray());
        outState.putLongArray(KEY_SELECTED_INTERRUPTIONS, mAdapter.getSelectedInterruptionsArray());
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mRingtoneTitleCache);
        outState.putParcelable(KEY_DELETED_INTERRUPTION, mDeletedInterruption);
        outState.putBoolean(KEY_UNDO_SHOWING, mUndoShowing);
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mAdapter.getPreviousDaysOfWeekMap());
        outState.putParcelable(KEY_SELECTED_INTERRUPTION, mSelectedInterruption);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the user places the app in the background by pressing "home",
        // dismiss the toast bar. However, since there is no way to determine if
        // home was pressed, just dismiss any existing toast bar when restarting
        // the app.
        hideUndoBar(false, null);
    }


    // Callback used by TimePickerDialog
    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
        if (mSelectedInterruption == null) {
            // If mSelectedInterruption is null then we're creating a new interruption.
            Interruption a = new Interruption();
            a.alert = RingtoneManager.getActualDefaultRingtoneUri(getActivity(),
                    RingtoneManager.TYPE_NOTIFICATION);
            if (a.alert == null) {
                //a.alert = Uri.parse("content://settings/system/alarm_alert");
            }
            a.hour = hourOfDay;
            a.minutes = minute;
            a.enabled = true;
            mAddedInterruption = a;
            asyncAddInterruption(a);
        } else {
            mSelectedInterruption.hour = hourOfDay;
            mSelectedInterruption.minutes = minute;
            mSelectedInterruption.enabled = true;
            mScrollToInterruptionId = mSelectedInterruption.id;
            asyncUpdateInterruption(mSelectedInterruption, true);
            mSelectedInterruption = null;
        }
    }

    private void showLabelDialog(final Interruption interruption) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(interruption, interruption.label, getTag());
        newFragment.show(ft, "label_dialog");
    }

    public void setLabel(Interruption interruption, String label) {
        interruption.label = label;
        asyncUpdateInterruption(interruption, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Interruption.getInterruptionsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor data) {
        mAdapter.swapCursor(data);
        if (mScrollToInterruptionId != INVALID_ID) {
            scrollToInterruption(mScrollToInterruptionId);
            mScrollToInterruptionId = INVALID_ID;
        }
    }


    /**
     * Scroll to interruption with given interruption id.
     *
     * @param interruptionId The interruption id to scroll to.
     */
    private void scrollToInterruption(long interruptionId) {
        int interruptionPosition = -1;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            long id = mAdapter.getItemId(i);
            if (id == interruptionId) {
                interruptionPosition = i;
                break;
            }
        }

        if (interruptionPosition >= 0) {
            mAdapter.setNewInterruption(interruptionId);
            mInterruptionsList.smoothScrollToPositionFromTop(interruptionPosition, 0);
        } else {
            // Trying to display a deleted interruption should only happen from a missed notification for
            // an interruption that has been marked deleted after use.
            Context context = getActivity().getApplicationContext();
            Toast toast = Toast.makeText(context, R.string.missed_alarm_has_been_deleted,
                    Toast.LENGTH_LONG);
            ToastMaster.setToast(toast);
            toast.show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    private void launchNotificationPicker(Interruption interruption) {
        mSelectedInterruption = interruption;
        Uri oldRingtone = Interruption.NO_NOTIFICATION_URI.equals(interruption.alert) ? null : interruption.alert;
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, oldRingtone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    private void saveRingtoneUri(Intent intent) {
        Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
            uri = Interruption.NO_NOTIFICATION_URI;
        }
        mSelectedInterruption.alert = uri;

        // Save the last selected ringtone as the default for new interruptions
        if (!Interruption.NO_NOTIFICATION_URI.equals(uri)) {
            RingtoneManager.setActualDefaultRingtoneUri(
                    getActivity(), RingtoneManager.TYPE_NOTIFICATION, uri);
        }
        asyncUpdateInterruption(mSelectedInterruption, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_RINGTONE:
                    saveRingtoneUri(data);
                    break;
                default:
                    LogUtils.w("Unhandled request code in onActivityResult: " + requestCode);
            }
        }
    }

    public class InterruptionItemAdapter extends CursorAdapter {
        private final Context mContext;
        private final LayoutInflater mFactory;
        private final String[] mShortWeekDayStrings;
        private final String[] mLongWeekDayStrings;
        private final int mColorLit;
        private final int mColorDim;
        private final Typeface mRobotoNormal;
        private final ListView mList;

        private long mExpandedId;
        private ItemHolder mExpandedItemHolder;
        private final HashSet<Long> mRepeatChecked = new HashSet<Long>();
        private final HashSet<Long> mSelectedInterruptions = new HashSet<Long>();
        private Bundle mPreviousDaysOfWeekMap = new Bundle();

        private final boolean mHasVibrator;
        private final int mCollapseExpandHeight;

        // This determines the order in which it is shown and processed in the UI.
        private final int[] DAY_ORDER = new int[] {
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
        };

        public class ItemHolder {

            // views for optimization
            LinearLayout interruptionItem;
            TextTime clock;
            TextView tomorrowLabel;
            Switch onoff;
            TextView daysOfWeek;
            TextView label;
            ImageButton delete;
            View expandArea;
            View summary;
            TextView clickableLabel;
            CheckBox repeat;
            LinearLayout repeatDays;
            Button[] dayButtons = new Button[7];
            CheckBox vibrate;
            TextView ringtone;
            View hairLine;
            View arrow;
            View collapseExpandArea;

            // Other states
            Interruption interruption;
        }

        // Used for scrolling an expanded item in the list to make sure it is fully visible.
        private long mScrollInterruptionId = InterruptionFragment.INVALID_ID;
        private final Runnable mScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (mScrollInterruptionId != InterruptionFragment.INVALID_ID) {
                    View v = getViewById(mScrollInterruptionId);
                    if (v != null) {
                        Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        mList.requestChildRectangleOnScreen(v, rect, false);
                    }
                    mScrollInterruptionId = InterruptionFragment.INVALID_ID;
                }
            }
        };

        public InterruptionItemAdapter(Context context, long expandedId, long[] repeatCheckedIds,
                                long[] selectedInterruptions, Bundle previousDaysOfWeekMap, ListView list) {
            super(context, null, 0);
            mContext = context;
            mFactory = LayoutInflater.from(context);
            mList = list;

            DateFormatSymbols dfs = new DateFormatSymbols();
            mShortWeekDayStrings = Utils.getShortWeekdays();
            mLongWeekDayStrings = dfs.getWeekdays();

            Resources res = mContext.getResources();
            mColorLit = res.getColor(R.color.clock_white);
            mColorDim = res.getColor(R.color.clock_gray);

            mRobotoNormal = Typeface.create("sans-serif", Typeface.NORMAL);

            mExpandedId = expandedId;
            if (repeatCheckedIds != null) {
                buildHashSetFromArray(repeatCheckedIds, mRepeatChecked);
            }
            if (previousDaysOfWeekMap != null) {
                mPreviousDaysOfWeekMap = previousDaysOfWeekMap;
            }
            if (selectedInterruptions != null) {
                buildHashSetFromArray(selectedInterruptions, mSelectedInterruptions);
            }

            mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .hasVibrator();

            mCollapseExpandHeight = (int) res.getDimension(R.dimen.collapse_expand_height);
        }

        public void removeSelectedId(int id) {
            mSelectedInterruptions.remove(id);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!getCursor().moveToPosition(position)) {
                // May happen if the last interruption was deleted and the cursor refreshed while the
                // list is updated.
                LogUtils.v("couldn't move cursor to position " + position);
                return null;
            }
            View v;
            if (convertView == null) {
                v = newView(mContext, getCursor(), parent);
            } else {
                v = convertView;
            }
            bindView(v, mContext, getCursor());
            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mFactory.inflate(R.layout.interruption_time, parent, false);
            setNewHolder(view);
            return view;
        }

        /**
         * In addition to changing the data set for the interruption list, swapCursor is now also
         * responsible for preparing the transition for any added/removed items.
         */
        @Override
        public synchronized Cursor swapCursor(Cursor cursor) {
            if (mAddedInterruption != null || mDeletedInterruption != null) {
                TransitionManager.beginDelayedTransition(mInterruptionsList, mAddRemoveTransition);
            }

            final Cursor c = super.swapCursor(cursor);

            mAddedInterruption = null;
            mDeletedInterruption= null;

            return c;
        }

        private void setNewHolder(View view) {
            // standard view holder optimization
            final ItemHolder holder = new ItemHolder();
            holder.interruptionItem = (LinearLayout) view.findViewById(R.id.alarm_item);
            holder.tomorrowLabel = (TextView) view.findViewById(R.id.tomorrowLabel);
            holder.clock = (TextTime) view.findViewById(R.id.digital_clock);
            holder.onoff = (Switch) view.findViewById(R.id.onoff);
            holder.onoff.setTypeface(mRobotoNormal);
            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
            holder.label = (TextView) view.findViewById(R.id.label);
            holder.delete = (ImageButton) view.findViewById(R.id.delete);
            holder.summary = view.findViewById(R.id.summary);
            holder.expandArea = view.findViewById(R.id.expand_area);
            holder.hairLine = view.findViewById(R.id.hairline);
            holder.arrow = view.findViewById(R.id.arrow);
            holder.repeat = (CheckBox) view.findViewById(R.id.repeat_onoff);
            holder.clickableLabel = (TextView) view.findViewById(R.id.edit_label);
            holder.repeatDays = (LinearLayout) view.findViewById(R.id.repeat_days);
            holder.collapseExpandArea = view.findViewById(R.id.collapse_expand);

            // Build button for each day.
            for (int i = 0; i < 7; i++) {
                final Button dayButton = (Button) mFactory.inflate(
                        R.layout.day_button, holder.repeatDays, false /* attachToRoot */);
                dayButton.setText(mShortWeekDayStrings[i]);
                dayButton.setContentDescription(mLongWeekDayStrings[DAY_ORDER[i]]);
                holder.repeatDays.addView(dayButton);
                holder.dayButtons[i] = dayButton;
            }
            holder.vibrate = (CheckBox) view.findViewById(R.id.vibrate_onoff);
            holder.ringtone = (TextView) view.findViewById(R.id.choose_ringtone);

            view.setTag(holder);
        }

        @Override
        public void bindView(final View view, Context context, final Cursor cursor) {
            final Interruption interruption = new Interruption(cursor);
            Object tag = view.getTag();
            if (tag == null) {
                // The view was converted but somehow lost its tag.
                setNewHolder(view);
            }
            final ItemHolder itemHolder = (ItemHolder) tag;
            itemHolder.interruption = interruption;

            // We must unset the listener first because this maybe a recycled view so changing the
            // state would affect the wrong interruption.
            itemHolder.onoff.setOnCheckedChangeListener(null);
            itemHolder.onoff.setChecked(interruption.enabled);

            if (mSelectedInterruptions.contains(itemHolder.interruption.id)) {
                setInterruptionItemBackgroundAndElevation(itemHolder.interruptionItem, true /* expanded */);
                setDigitalTimeAlpha(itemHolder, true);
                itemHolder.onoff.setEnabled(false);
            } else {
                itemHolder.onoff.setEnabled(true);
                setInterruptionItemBackgroundAndElevation(itemHolder.interruptionItem, false /* expanded */);
                setDigitalTimeAlpha(itemHolder, itemHolder.onoff.isChecked());
            }
            itemHolder.clock.setFormat(
                    (int)mContext.getResources().getDimension(R.dimen.alarm_label_size));
            itemHolder.clock.setTime(interruption.hour, interruption.minutes);
            itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSelectedInterruption = itemHolder.interruption;
                    InterruptionUtils.showTimeEditDialog(InterruptionFragment.this, interruption);
                    expandInterruption(itemHolder, true);
                    itemHolder.interruptionItem.post(mScrollRunnable);
                }
            });

            final CompoundButton.OnCheckedChangeListener onOffListener =
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton,
                                                     boolean checked) {
                            if (checked != interruption.enabled) {
                                setDigitalTimeAlpha(itemHolder, checked);
                                interruption.enabled = checked;
                                asyncUpdateInterruption(interruption, interruption.enabled);
                            }
                        }
                    };

            if (mRepeatChecked.contains(interruption.id) || itemHolder.interruption.daysOfWeek.isRepeating()) {
                itemHolder.tomorrowLabel.setVisibility(View.GONE);
            } else {
                itemHolder.tomorrowLabel.setVisibility(View.VISIBLE);
                final Resources resources = getResources();
                final String labelText = isTomorrow(interruption) ?
                        resources.getString(R.string.alarm_tomorrow) :
                        resources.getString(R.string.alarm_today);
                itemHolder.tomorrowLabel.setText(labelText);
            }
            itemHolder.onoff.setOnCheckedChangeListener(onOffListener);

            boolean expanded = isInterruptionExpanded(interruption);
            if (expanded) {
                mExpandedItemHolder = itemHolder;
            }
            itemHolder.expandArea.setVisibility(expanded? View.VISIBLE : View.GONE);
            itemHolder.delete.setVisibility(expanded ? View.VISIBLE : View.GONE);
            itemHolder.summary.setVisibility(expanded? View.GONE : View.VISIBLE);
            itemHolder.hairLine.setVisibility(expanded ? View.GONE : View.VISIBLE);
            itemHolder.arrow.setRotation(expanded ? ROTATE_180_DEGREE : 0);

            // Set the repeat text or leave it blank if it does not repeat.
            final String daysOfWeekStr =
                    interruption.daysOfWeek.toString(InterruptionFragment.this.getActivity(), false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr);
                itemHolder.daysOfWeek.setContentDescription(interruption.daysOfWeek.toAccessibilityString(
                        InterruptionFragment.this.getActivity()));
                itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
                itemHolder.daysOfWeek.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandInterruption(itemHolder, true);
                        itemHolder.interruptionItem.post(mScrollRunnable);
                    }
                });

            } else {
                itemHolder.daysOfWeek.setVisibility(View.GONE);
            }

            if (interruption.label != null && interruption.label.length() != 0) {
                itemHolder.label.setText(interruption.label + "  ");
                itemHolder.label.setVisibility(View.VISIBLE);
                itemHolder.label.setContentDescription(
                        mContext.getResources().getString(R.string.label_description) + " "
                                + interruption.label);
                itemHolder.label.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandInterruption(itemHolder, true);
                        itemHolder.interruptionItem.post(mScrollRunnable);
                    }
                });
            } else {
                itemHolder.label.setVisibility(View.GONE);
            }

            itemHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDeletedInterruption = interruption;
                    mRepeatChecked.remove(interruption.id);
                    asyncDeleteInterruption(interruption);
                }
            });

            if (expanded) {
                expandInterruption(itemHolder, false);
            }

            itemHolder.interruptionItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isInterruptionExpanded(interruption)) {
                        collapseInterruption(itemHolder, true);
                    } else {
                        expandInterruption(itemHolder, true);
                    }
                }
            });
        }

        private void setInterruptionItemBackgroundAndElevation(LinearLayout layout, boolean expanded) {
            if (expanded) {
                layout.setBackgroundColor(getTintedBackgroundColor());
                layout.setElevation(INTERRUPTION_ELEVATION);
            } else {
                layout.setBackgroundResource(R.drawable.alarm_background_normal);
                layout.setElevation(0);
            }
        }

        private int getTintedBackgroundColor() {
            final int c = Utils.getCurrentHourColor();
            final int red = Color.red(c) + (int) (TINTED_LEVEL * (255 - Color.red(c)));
            final int green = Color.green(c) + (int) (TINTED_LEVEL * (255 - Color.green(c)));
            final int blue = Color.blue(c) + (int) (TINTED_LEVEL * (255 - Color.blue(c)));
            return Color.rgb(red, green, blue);
        }

        private boolean isTomorrow(Interruption interruption) {
            final Calendar now = Calendar.getInstance();
            final int interruptionHour = interruption.hour;
            final int currHour = now.get(Calendar.HOUR_OF_DAY);
            return interruptionHour < currHour ||
                    (interruptionHour == currHour && interruption.minutes < now.get(Calendar.MINUTE));
        }

        private void bindExpandArea(final ItemHolder itemHolder, final Interruption interruption) {
            // Views in here are not bound until the item is expanded.

            if (interruption.label != null && interruption.label.length() > 0) {
                itemHolder.clickableLabel.setText(interruption.label);
            } else {
                itemHolder.clickableLabel.setText(R.string.label);
            }

            itemHolder.clickableLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLabelDialog(interruption);
                }
            });

            if (mRepeatChecked.contains(interruption.id) || itemHolder.interruption.daysOfWeek.isRepeating()) {
                itemHolder.repeat.setChecked(true);
                itemHolder.repeatDays.setVisibility(View.VISIBLE);
            } else {
                itemHolder.repeat.setChecked(false);
                itemHolder.repeatDays.setVisibility(View.GONE);
            }
            itemHolder.repeat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Animate the resulting layout changes.
                    TransitionManager.beginDelayedTransition(mList, mRepeatTransition);

                    final boolean checked = ((CheckBox) view).isChecked();
                    if (checked) {
                        // Show days
                        itemHolder.repeatDays.setVisibility(View.VISIBLE);
                        mRepeatChecked.add(interruption.id);

                        // Set all previously set days
                        // or
                        // Set all days if no previous.
                        final int bitSet = mPreviousDaysOfWeekMap.getInt("" + interruption.id);
                        interruption.daysOfWeek.setBitSet(bitSet);
                        if (!interruption.daysOfWeek.isRepeating()) {
                            interruption.daysOfWeek.setDaysOfWeek(true, DAY_ORDER);
                        }
                        updateDaysOfWeekButtons(itemHolder, interruption.daysOfWeek);
                    } else {
                        // Hide days
                        itemHolder.repeatDays.setVisibility(View.GONE);
                        mRepeatChecked.remove(interruption.id);

                        // Remember the set days in case the user wants it back.
                        final int bitSet = interruption.daysOfWeek.getBitSet();
                        mPreviousDaysOfWeekMap.putInt("" + interruption.id, bitSet);

                        // Remove all repeat days
                        interruption.daysOfWeek.clearAllDays();
                    }

                    asyncUpdateInterruption(interruption, false);
                }
            });

            updateDaysOfWeekButtons(itemHolder, interruption.daysOfWeek);
            for (int i = 0; i < 7; i++) {
                final int buttonIndex = i;

                itemHolder.dayButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final boolean isActivated =
                                itemHolder.dayButtons[buttonIndex].isActivated();
                        interruption.daysOfWeek.setDaysOfWeek(!isActivated, DAY_ORDER[buttonIndex]);
                        if (!isActivated) {
                            turnOnDayOfWeek(itemHolder, buttonIndex);
                        } else {
                            turnOffDayOfWeek(itemHolder, buttonIndex);

                            // See if this was the last day, if so, un-check the repeat box.
                            if (!interruption.daysOfWeek.isRepeating()) {
                                // Animate the resulting layout changes.
                                TransitionManager.beginDelayedTransition(mList, mRepeatTransition);

                                itemHolder.repeat.setChecked(false);
                                itemHolder.repeatDays.setVisibility(View.GONE);
                                mRepeatChecked.remove(interruption.id);

                                // Set history to no days, so it will be everyday when repeat is
                                // turned back on
                                mPreviousDaysOfWeekMap.putInt("" + interruption.id,
                                        DaysOfWeek.NO_DAYS_SET);
                            }
                        }
                        asyncUpdateInterruption(interruption, false);
                    }
                });
            }

            if (!mHasVibrator) {
                itemHolder.vibrate.setVisibility(View.INVISIBLE);
            } else {
                itemHolder.vibrate.setVisibility(View.VISIBLE);
                if (!interruption.vibrate) {
                    itemHolder.vibrate.setChecked(false);
                } else {
                    itemHolder.vibrate.setChecked(true);
                }
            }

            itemHolder.vibrate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final boolean checked = ((CheckBox) v).isChecked();
                    interruption.vibrate = checked;
                    asyncUpdateInterruption(interruption, false);
                }
            });

            final String ringtone;
            if (Interruption.NO_NOTIFICATION_URI.equals(interruption.alert)) {
                ringtone = mContext.getResources().getString(R.string.silent_alarm_summary);
            } else {
                ringtone = getRingToneTitle(interruption.alert);
            }
            itemHolder.ringtone.setText(ringtone);
            itemHolder.ringtone.setContentDescription(
                    mContext.getResources().getString(R.string.ringtone_description) + " "
                            + ringtone);
            itemHolder.ringtone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchNotificationPicker(interruption);
                }
            });
        }

        // Sets the alpha of the digital time display. This gives a visual effect
        // for enabled/disabled interruption while leaving the on/off switch more visible
        private void setDigitalTimeAlpha(ItemHolder holder, boolean enabled) {
            float alpha = enabled ? 1f : 0.69f;
            holder.clock.setAlpha(alpha);
        }

        private void updateDaysOfWeekButtons(ItemHolder holder, DaysOfWeek daysOfWeek) {
            HashSet<Integer> setDays = daysOfWeek.getSetDays();
            for (int i = 0; i < 7; i++) {
                if (setDays.contains(DAY_ORDER[i])) {
                    turnOnDayOfWeek(holder, i);
                } else {
                    turnOffDayOfWeek(holder, i);
                }
            }
        }

        public void toggleSelectState(View v) {
            // long press could be on the parent view or one of its childs, so find the parent view
            v = getTopParent(v);
            if (v != null) {
                long id = ((ItemHolder)v.getTag()).interruption.id;
                if (mSelectedInterruptions.contains(id)) {
                    mSelectedInterruptions.remove(id);
                } else {
                    mSelectedInterruptions.add(id);
                }
            }
        }

        private View getTopParent(View v) {
            while (v != null && v.getId() != R.id.alarm_item) {
                v = (View) v.getParent();
            }
            return v;
        }

        public int getSelectedItemsNum() {
            return mSelectedInterruptions.size();
        }

        private void turnOffDayOfWeek(ItemHolder holder, int dayIndex) {
            final Button dayButton = holder.dayButtons[dayIndex];
            dayButton.setActivated(false);
            dayButton.setTextColor(getResources().getColor(R.color.clock_white));
        }

        private void turnOnDayOfWeek(ItemHolder holder, int dayIndex) {
            final Button dayButton = holder.dayButtons[dayIndex];
            dayButton.setActivated(true);
            dayButton.setTextColor(Utils.getCurrentHourColor());
        }


        /**
         * Does a read-through cache for ringtone titles.
         *
         * @param uri The uri of the ringtone.
         * @return The ringtone title. {@literal null} if no matching ringtone found.
         */
        private String getRingToneTitle(Uri uri) {
            // Try the cache first
            String title = mRingtoneTitleCache.getString(uri.toString());
            if (title == null) {
                // This is slow because a media player is created during Ringtone object creation.
                Ringtone ringTone = RingtoneManager.getRingtone(mContext, uri);
                title = ringTone.getTitle(mContext);
                if (title != null) {
                    mRingtoneTitleCache.putString(uri.toString(), title);
                }
            }
            return title;
        }

        public void setNewInterruption(long interruptionId) {
            mExpandedId = interruptionId;
        }

        /**
         * Expands the interruption for editing.
         *
         * @param itemHolder The item holder instance.
         */
        private void expandInterruption(final ItemHolder itemHolder, boolean animate) {
            // Skip animation later if item is already expanded
            animate &= mExpandedId != itemHolder.interruption.id;

            if (mExpandedItemHolder != null
                    && mExpandedItemHolder != itemHolder
                    && mExpandedId != itemHolder.interruption.id) {
                // Only allow one interruption to expand at a time.
                collapseInterruption(mExpandedItemHolder, animate);
            }

            bindExpandArea(itemHolder, itemHolder.interruption);

            mExpandedId = itemHolder.interruption.id;
            mExpandedItemHolder = itemHolder;

            // Scroll the view to make sure it is fully viewed
            mScrollInterruptionId = itemHolder.interruption.id;

            // Save the starting height so we can animate from this value.
            final int startingHeight = itemHolder.interruptionItem.getHeight();

            // Set the expand area to visible so we can measure the height to animate to.
            setInterruptionItemBackgroundAndElevation(itemHolder.interruptionItem, true /* expanded */);
            itemHolder.expandArea.setVisibility(View.VISIBLE);
            itemHolder.delete.setVisibility(View.VISIBLE);

            if (!animate) {
                // Set the "end" layout and don't do the animation.
                itemHolder.arrow.setRotation(ROTATE_180_DEGREE);
                return;
            }

            // Add an onPreDrawListener, which gets called after measurement but before the draw.
            // This way we can check the height we need to animate to before any drawing.
            // Note the series of events:
            //  * expandArea is set to VISIBLE, which causes a layout pass
            //  * the view is measured, and our onPreDrawListener is called
            //  * we set up the animation using the start and end values.
            //  * the height is set back to the starting point so it can be animated down.
            //  * request another layout pass.
            //  * return false so that onDraw() is not called for the single frame before
            //    the animations have started.
            final ViewTreeObserver observer = mInterruptionsList.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // We don't want to continue getting called for every listview drawing.
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }
                    // Calculate some values to help with the animation.
                    final int endingHeight = itemHolder.interruptionItem.getHeight();
                    final int distance = endingHeight - startingHeight;
                    final int collapseHeight = itemHolder.collapseExpandArea.getHeight();

                    // Set the height back to the start state of the animation.
                    itemHolder.interruptionItem.getLayoutParams().height = startingHeight;
                    // To allow the expandArea to glide in with the expansion animation, set a
                    // negative top margin, which will animate down to a margin of 0 as the height
                    // is increased.
                    // Note that we need to maintain the bottom margin as a fixed value (instead of
                    // just using a listview, to allow for a flatter hierarchy) to fit the bottom
                    // bar underneath.
                    FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                            itemHolder.expandArea.getLayoutParams();
                    expandParams.setMargins(0, -distance, 0, collapseHeight);
                    itemHolder.interruptionItem.requestLayout();

                    // Set up the animator to animate the expansion.
                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
                            .setDuration(EXPAND_DURATION);
                    animator.setInterpolator(mExpandInterpolator);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            Float value = (Float) animator.getAnimatedValue();

                            // For each value from 0 to 1, animate the various parts of the layout.
                            itemHolder.interruptionItem.getLayoutParams().height =
                                    (int) (value * distance + startingHeight);
                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(
                                    0, (int) -((1 - value) * distance), 0, collapseHeight);
                            itemHolder.arrow.setRotation(ROTATE_180_DEGREE * value);
                            itemHolder.summary.setAlpha(1 - value);
                            itemHolder.hairLine.setAlpha(1 - value);

                            itemHolder.interruptionItem.requestLayout();
                        }
                    });
                    // Set everything to their final values when the animation's done.
                    animator.addListener(new AnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Set it back to wrap content since we'd explicitly set the height.
                            itemHolder.interruptionItem.getLayoutParams().height =
                                    LayoutParams.WRAP_CONTENT;
                            itemHolder.arrow.setRotation(ROTATE_180_DEGREE);
                            itemHolder.summary.setVisibility(View.GONE);
                            itemHolder.hairLine.setVisibility(View.GONE);
                            itemHolder.delete.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            // TODO we may have to deal with cancelations of the animation.
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) { }
                        @Override
                        public void onAnimationStart(Animator animation) { }
                    });
                    animator.start();

                    // Return false so this draw does not occur to prevent the final frame from
                    // being drawn for the single frame before the animations start.
                    return false;
                }
            });
        }

        private boolean isInterruptionExpanded(Interruption interruption) {
            return mExpandedId == interruption.id;
        }

        private void collapseInterruption(final ItemHolder itemHolder, boolean animate) {
            mExpandedId = InterruptionFragment.INVALID_ID;
            mExpandedItemHolder = null;

            // Save the starting height so we can animate from this value.
            final int startingHeight = itemHolder.interruptionItem.getHeight();

            // Set the expand area to gone so we can measure the height to animate to.
            setInterruptionItemBackgroundAndElevation(itemHolder.interruptionItem, false /* expanded */);
            itemHolder.expandArea.setVisibility(View.GONE);

            if (!animate) {
                // Set the "end" layout and don't do the animation.
                itemHolder.arrow.setRotation(0);
                itemHolder.hairLine.setTranslationY(0);
                return;
            }

            // Add an onPreDrawListener, which gets called after measurement but before the draw.
            // This way we can check the height we need to animate to before any drawing.
            // Note the series of events:
            //  * expandArea is set to GONE, which causes a layout pass
            //  * the view is measured, and our onPreDrawListener is called
            //  * we set up the animation using the start and end values.
            //  * expandArea is set to VISIBLE again so it can be shown animating.
            //  * request another layout pass.
            //  * return false so that onDraw() is not called for the single frame before
            //    the animations have started.
            final ViewTreeObserver observer = mInterruptionsList.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }

                    // Calculate some values to help with the animation.
                    final int endingHeight = itemHolder.interruptionItem.getHeight();
                    final int distance = endingHeight - startingHeight;

                    // Re-set the visibilities for the start state of the animation.
                    itemHolder.expandArea.setVisibility(View.VISIBLE);
                    itemHolder.delete.setVisibility(View.GONE);
                    itemHolder.summary.setVisibility(View.VISIBLE);
                    itemHolder.hairLine.setVisibility(View.VISIBLE);
                    itemHolder.summary.setAlpha(1);

                    // Set up the animator to animate the expansion.
                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
                            .setDuration(COLLAPSE_DURATION);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            Float value = (Float) animator.getAnimatedValue();

                            // For each value from 0 to 1, animate the various parts of the layout.
                            itemHolder.interruptionItem.getLayoutParams().height =
                                    (int) (value * distance + startingHeight);
                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(
                                    0, (int) (value * distance), 0, mCollapseExpandHeight);
                            itemHolder.arrow.setRotation(ROTATE_180_DEGREE * (1 - value));
                            itemHolder.delete.setAlpha(value);
                            itemHolder.summary.setAlpha(value);
                            itemHolder.hairLine.setAlpha(value);

                            itemHolder.interruptionItem.requestLayout();
                        }
                    });
                    animator.setInterpolator(mCollapseInterpolator);
                    // Set everything to their final values when the animation's done.
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Set it back to wrap content since we'd explicitly set the height.
                            itemHolder.interruptionItem.getLayoutParams().height =
                                    LayoutParams.WRAP_CONTENT;

                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(0, 0, 0, mCollapseExpandHeight);

                            itemHolder.expandArea.setVisibility(View.GONE);
                            itemHolder.arrow.setRotation(0);
                        }
                    });
                    animator.start();

                    return false;
                }
            });
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        private View getViewById(long id) {
            for (int i = 0; i < mList.getCount(); i++) {
                View v = mList.getChildAt(i);
                if (v != null) {
                    ItemHolder h = (ItemHolder)(v.getTag());
                    if (h != null && h.interruption.id == id) {
                        return v;
                    }
                }
            }
            return null;
        }

        public long getExpandedId() {
            return mExpandedId;
        }

        public long[] getSelectedInterruptionsArray() {
            int index = 0;
            long[] ids = new long[mSelectedInterruptions.size()];
            for (long id : mSelectedInterruptions) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public long[] getRepeatArray() {
            int index = 0;
            long[] ids = new long[mRepeatChecked.size()];
            for (long id : mRepeatChecked) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public Bundle getPreviousDaysOfWeekMap() {
            return mPreviousDaysOfWeekMap;
        }

        private void buildHashSetFromArray(long[] ids, HashSet<Long> set) {
            for (long id : ids) {
                set.add(id);
            }
        }
    }

    private void startCreatingInterruption() {
        // Set the "selected" alarm as null, and we'll create the new one when the timepicker
        // comes back.
        mSelectedInterruption = null;
        InterruptionUtils.showTimeEditDialog(this, null);
    }

    private static InterruptionInstance setupAlarmInstance(Context context, Interruption interruption) {
        ContentResolver cr = context.getContentResolver();
        InterruptionInstance newInstance = interruption.createInstanceAfter(Calendar.getInstance());
        newInstance = InterruptionInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        InterruptionStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private void asyncDeleteInterruption(final Interruption interruption) {
        final Context context = InterruptionFragment.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                // Activity may be closed at this point , make sure data is still valid
                if (context != null && interruption != null) {
                    ContentResolver cr = context.getContentResolver();
                    InterruptionStateManager.deleteAllInstances(context, interruption.id);
                    Interruption.deleteInterruption(cr, interruption.id);
                    sInterruptionHelperExtensions.deleteInterruption(
                            InterruptionFragment.this.getActivity().getApplicationContext(), interruption.id);
                }
                return null;
            }
        };
        mUndoShowing = true;
        deleteTask.execute();
    }

    private void asyncAddInterruption(final Interruption interruption) {
        final Context context = InterruptionFragment.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, InterruptionInstance> updateTask =
                new AsyncTask<Void, Void, InterruptionInstance>() {
                    @Override
                    protected InterruptionInstance doInBackground(Void... parameters) {
                        if (context != null && interruption != null) {
                            ContentResolver cr = context.getContentResolver();

                            // Add alarm to db
                            Interruption newInterruption = Interruption.addInterruption(cr, interruption);
                            mScrollToInterruptionId = newInterruption.id;

                            // Create and add instance to db
                            if (newInterruption.enabled) {
                                sInterruptionHelperExtensions.addInterruption(
                                        InterruptionFragment.this.getActivity().getApplicationContext(),
                                        newInterruption);
                                return setupAlarmInstance(context, newInterruption);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(InterruptionInstance instance) {
                        if (instance != null) {
                            InterruptionUtils.popAlarmSetToast(context, instance.getInterruptionTime().getTimeInMillis());
                        }
                    }
                };
        updateTask.execute();
    }

    private void asyncUpdateInterruption(final Interruption interruption, final boolean popToast) {
        final Context context = InterruptionFragment.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, InterruptionInstance> updateTask =
                new AsyncTask<Void, Void, InterruptionInstance>() {
                    @Override
                    protected InterruptionInstance doInBackground(Void ... parameters) {
                        ContentResolver cr = context.getContentResolver();

                        // Dismiss all old instances
                        InterruptionStateManager.deleteAllInstances(context, interruption.id);

                        // Update alarm
                        Interruption.updateInterruption(cr, interruption);
                        if (interruption.enabled) {
                            return setupAlarmInstance(context, interruption);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(InterruptionInstance instance) {
                        if (popToast && instance != null) {
                            InterruptionUtils.popAlarmSetToast(context, instance.getInterruptionTime().getTimeInMillis());
                        }
                    }
                };
        updateTask.execute();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideUndoBar(true, event);
        return false;
    }

    @Override
    public void onFabClick(View view){
        hideUndoBar(true, null);
        startCreatingInterruption();
    }

    @Override
    public void setFabAppearance() {
        final InterruptionHelper activity = (InterruptionHelper) getActivity();
        if (mFab == null || activity.getSelectedTab() != InterruptionHelper.INTERRUPTION_TAB_INDEX) {
            return;
        }
        mFab.setVisibility(View.VISIBLE);
        mFab.setImageResource(R.drawable.ic_fab_plus);
        mFab.setContentDescription(getString(R.string.button_alarms));
    }

    @Override
    public void setLeftRightButtonAppearance() {
        final InterruptionHelper activity = (InterruptionHelper) getActivity();
        if (mLeftButton == null || mRightButton == null ||
                activity.getSelectedTab() != InterruptionHelper.INTERRUPTION_TAB_INDEX) {
            return;
        }
        mLeftButton.setVisibility(View.INVISIBLE);
        mRightButton.setVisibility(View.INVISIBLE);
    }
}
