package com.mk.interruptionshelper;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Calendar;
import java.util.HashSet;

/**
 * Created by dhdev_000 on 2015/7/21.
 */
public class AlarmItemAdapter {
    public class AlarmItemAdapter extends CursorAdapter {
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
        private final HashSet<Long> mSelectedAlarms = new HashSet<Long>();
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
            LinearLayout alarmItem;
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
            Alarm alarm;
        }

        // Used for scrolling an expanded item in the list to make sure it is fully visible.
        private long mScrollAlarmId = AlarmClockFragment.INVALID_ID;
        private final Runnable mScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (mScrollAlarmId != AlarmClockFragment.INVALID_ID) {
                    View v = getViewById(mScrollAlarmId);
                    if (v != null) {
                        Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        mList.requestChildRectangleOnScreen(v, rect, false);
                    }
                    mScrollAlarmId = AlarmClockFragment.INVALID_ID;
                }
            }
        };

        public AlarmItemAdapter(Context context, long expandedId, long[] repeatCheckedIds,
                                long[] selectedAlarms, Bundle previousDaysOfWeekMap, ListView list) {
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
            if (selectedAlarms != null) {
                buildHashSetFromArray(selectedAlarms, mSelectedAlarms);
            }

            mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .hasVibrator();

            mCollapseExpandHeight = (int) res.getDimension(R.dimen.collapse_expand_height);
        }

        public void removeSelectedId(int id) {
            mSelectedAlarms.remove(id);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!getCursor().moveToPosition(position)) {
                // May happen if the last alarm was deleted and the cursor refreshed while the
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
            final View view = mFactory.inflate(R.layout.alarm_time, parent, false);
            setNewHolder(view);
            return view;
        }

        /**
         * In addition to changing the data set for the alarm list, swapCursor is now also
         * responsible for preparing the transition for any added/removed items.
         */
        @Override
        public synchronized Cursor swapCursor(Cursor cursor) {
            if (mAddedAlarm != null || mDeletedAlarm != null) {
                TransitionManager.beginDelayedTransition(mAlarmsList, mAddRemoveTransition);
            }

            final Cursor c = super.swapCursor(cursor);

            mAddedAlarm = null;
            mDeletedAlarm = null;

            return c;
        }

        private void setNewHolder(View view) {
            // standard view holder optimization
            final ItemHolder holder = new ItemHolder();
            holder.alarmItem = (LinearLayout) view.findViewById(R.id.alarm_item);
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
            final Alarm alarm = new Alarm(cursor);
            Object tag = view.getTag();
            if (tag == null) {
                // The view was converted but somehow lost its tag.
                setNewHolder(view);
            }
            final ItemHolder itemHolder = (ItemHolder) tag;
            itemHolder.alarm = alarm;

            // We must unset the listener first because this maybe a recycled view so changing the
            // state would affect the wrong alarm.
            itemHolder.onoff.setOnCheckedChangeListener(null);
            itemHolder.onoff.setChecked(alarm.enabled);

            if (mSelectedAlarms.contains(itemHolder.alarm.id)) {
                setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, true /* expanded */);
                setDigitalTimeAlpha(itemHolder, true);
                itemHolder.onoff.setEnabled(false);
            } else {
                itemHolder.onoff.setEnabled(true);
                setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, false /* expanded */);
                setDigitalTimeAlpha(itemHolder, itemHolder.onoff.isChecked());
            }
            itemHolder.clock.setFormat(
                    (int)mContext.getResources().getDimension(R.dimen.alarm_label_size));
            itemHolder.clock.setTime(alarm.hour, alarm.minutes);
            itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSelectedAlarm = itemHolder.alarm;
                    AlarmUtils.showTimeEditDialog(AlarmClockFragment.this, alarm);
                    expandAlarm(itemHolder, true);
                    itemHolder.alarmItem.post(mScrollRunnable);
                }
            });

            final CompoundButton.OnCheckedChangeListener onOffListener =
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton,
                                                     boolean checked) {
                            if (checked != alarm.enabled) {
                                setDigitalTimeAlpha(itemHolder, checked);
                                alarm.enabled = checked;
                                asyncUpdateAlarm(alarm, alarm.enabled);
                            }
                        }
                    };

            if (mRepeatChecked.contains(alarm.id) || itemHolder.alarm.daysOfWeek.isRepeating()) {
                itemHolder.tomorrowLabel.setVisibility(View.GONE);
            } else {
                itemHolder.tomorrowLabel.setVisibility(View.VISIBLE);
                final Resources resources = getResources();
                final String labelText = isTomorrow(alarm) ?
                        resources.getString(R.string.alarm_tomorrow) :
                        resources.getString(R.string.alarm_today);
                itemHolder.tomorrowLabel.setText(labelText);
            }
            itemHolder.onoff.setOnCheckedChangeListener(onOffListener);

            boolean expanded = isAlarmExpanded(alarm);
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
                    alarm.daysOfWeek.toString(AlarmClockFragment.this.getActivity(), false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr);
                itemHolder.daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(
                        AlarmClockFragment.this.getActivity()));
                itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
                itemHolder.daysOfWeek.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(mScrollRunnable);
                    }
                });

            } else {
                itemHolder.daysOfWeek.setVisibility(View.GONE);
            }

            if (alarm.label != null && alarm.label.length() != 0) {
                itemHolder.label.setText(alarm.label + "  ");
                itemHolder.label.setVisibility(View.VISIBLE);
                itemHolder.label.setContentDescription(
                        mContext.getResources().getString(R.string.label_description) + " "
                                + alarm.label);
                itemHolder.label.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(mScrollRunnable);
                    }
                });
            } else {
                itemHolder.label.setVisibility(View.GONE);
            }

            itemHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDeletedAlarm = alarm;
                    mRepeatChecked.remove(alarm.id);
                    asyncDeleteAlarm(alarm);
                }
            });

            if (expanded) {
                expandAlarm(itemHolder, false);
            }

            itemHolder.alarmItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isAlarmExpanded(alarm)) {
                        collapseAlarm(itemHolder, true);
                    } else {
                        expandAlarm(itemHolder, true);
                    }
                }
            });
        }

        private void setAlarmItemBackgroundAndElevation(LinearLayout layout, boolean expanded) {
            if (expanded) {
                layout.setBackgroundColor(getTintedBackgroundColor());
                layout.setElevation(ALARM_ELEVATION);
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

        private boolean isTomorrow(Alarm alarm) {
            final Calendar now = Calendar.getInstance();
            final int alarmHour = alarm.hour;
            final int currHour = now.get(Calendar.HOUR_OF_DAY);
            return alarmHour < currHour ||
                    (alarmHour == currHour && alarm.minutes < now.get(Calendar.MINUTE));
        }

        private void bindExpandArea(final ItemHolder itemHolder, final Alarm alarm) {
            // Views in here are not bound until the item is expanded.

            if (alarm.label != null && alarm.label.length() > 0) {
                itemHolder.clickableLabel.setText(alarm.label);
            } else {
                itemHolder.clickableLabel.setText(R.string.label);
            }

            itemHolder.clickableLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLabelDialog(alarm);
                }
            });

            if (mRepeatChecked.contains(alarm.id) || itemHolder.alarm.daysOfWeek.isRepeating()) {
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
                        mRepeatChecked.add(alarm.id);

                        // Set all previously set days
                        // or
                        // Set all days if no previous.
                        final int bitSet = mPreviousDaysOfWeekMap.getInt("" + alarm.id);
                        alarm.daysOfWeek.setBitSet(bitSet);
                        if (!alarm.daysOfWeek.isRepeating()) {
                            alarm.daysOfWeek.setDaysOfWeek(true, DAY_ORDER);
                        }
                        updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
                    } else {
                        // Hide days
                        itemHolder.repeatDays.setVisibility(View.GONE);
                        mRepeatChecked.remove(alarm.id);

                        // Remember the set days in case the user wants it back.
                        final int bitSet = alarm.daysOfWeek.getBitSet();
                        mPreviousDaysOfWeekMap.putInt("" + alarm.id, bitSet);

                        // Remove all repeat days
                        alarm.daysOfWeek.clearAllDays();
                    }

                    asyncUpdateAlarm(alarm, false);
                }
            });

            updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
            for (int i = 0; i < 7; i++) {
                final int buttonIndex = i;

                itemHolder.dayButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final boolean isActivated =
                                itemHolder.dayButtons[buttonIndex].isActivated();
                        alarm.daysOfWeek.setDaysOfWeek(!isActivated, DAY_ORDER[buttonIndex]);
                        if (!isActivated) {
                            turnOnDayOfWeek(itemHolder, buttonIndex);
                        } else {
                            turnOffDayOfWeek(itemHolder, buttonIndex);

                            // See if this was the last day, if so, un-check the repeat box.
                            if (!alarm.daysOfWeek.isRepeating()) {
                                // Animate the resulting layout changes.
                                TransitionManager.beginDelayedTransition(mList, mRepeatTransition);

                                itemHolder.repeat.setChecked(false);
                                itemHolder.repeatDays.setVisibility(View.GONE);
                                mRepeatChecked.remove(alarm.id);

                                // Set history to no days, so it will be everyday when repeat is
                                // turned back on
                                mPreviousDaysOfWeekMap.putInt("" + alarm.id,
                                        DaysOfWeek.NO_DAYS_SET);
                            }
                        }
                        asyncUpdateAlarm(alarm, false);
                    }
                });
            }

            if (!mHasVibrator) {
                itemHolder.vibrate.setVisibility(View.INVISIBLE);
            } else {
                itemHolder.vibrate.setVisibility(View.VISIBLE);
                if (!alarm.vibrate) {
                    itemHolder.vibrate.setChecked(false);
                } else {
                    itemHolder.vibrate.setChecked(true);
                }
            }

            itemHolder.vibrate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final boolean checked = ((CheckBox) v).isChecked();
                    alarm.vibrate = checked;
                    asyncUpdateAlarm(alarm, false);
                }
            });

            final String ringtone;
            if (Alarm.NO_RINGTONE_URI.equals(alarm.alert)) {
                ringtone = mContext.getResources().getString(R.string.silent_alarm_summary);
            } else {
                ringtone = getRingToneTitle(alarm.alert);
            }
            itemHolder.ringtone.setText(ringtone);
            itemHolder.ringtone.setContentDescription(
                    mContext.getResources().getString(R.string.ringtone_description) + " "
                            + ringtone);
            itemHolder.ringtone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchRingTonePicker(alarm);
                }
            });
        }

        // Sets the alpha of the digital time display. This gives a visual effect
        // for enabled/disabled alarm while leaving the on/off switch more visible
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
                long id = ((ItemHolder)v.getTag()).alarm.id;
                if (mSelectedAlarms.contains(id)) {
                    mSelectedAlarms.remove(id);
                } else {
                    mSelectedAlarms.add(id);
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
            return mSelectedAlarms.size();
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

        public void setNewAlarm(long alarmId) {
            mExpandedId = alarmId;
        }

        /**
         * Expands the alarm for editing.
         *
         * @param itemHolder The item holder instance.
         */
        private void expandAlarm(final ItemHolder itemHolder, boolean animate) {
            // Skip animation later if item is already expanded
            animate &= mExpandedId != itemHolder.alarm.id;

            if (mExpandedItemHolder != null
                    && mExpandedItemHolder != itemHolder
                    && mExpandedId != itemHolder.alarm.id) {
                // Only allow one alarm to expand at a time.
                collapseAlarm(mExpandedItemHolder, animate);
            }

            bindExpandArea(itemHolder, itemHolder.alarm);

            mExpandedId = itemHolder.alarm.id;
            mExpandedItemHolder = itemHolder;

            // Scroll the view to make sure it is fully viewed
            mScrollAlarmId = itemHolder.alarm.id;

            // Save the starting height so we can animate from this value.
            final int startingHeight = itemHolder.alarmItem.getHeight();

            // Set the expand area to visible so we can measure the height to animate to.
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, true /* expanded */);
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
            final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // We don't want to continue getting called for every listview drawing.
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }
                    // Calculate some values to help with the animation.
                    final int endingHeight = itemHolder.alarmItem.getHeight();
                    final int distance = endingHeight - startingHeight;
                    final int collapseHeight = itemHolder.collapseExpandArea.getHeight();

                    // Set the height back to the start state of the animation.
                    itemHolder.alarmItem.getLayoutParams().height = startingHeight;
                    // To allow the expandArea to glide in with the expansion animation, set a
                    // negative top margin, which will animate down to a margin of 0 as the height
                    // is increased.
                    // Note that we need to maintain the bottom margin as a fixed value (instead of
                    // just using a listview, to allow for a flatter hierarchy) to fit the bottom
                    // bar underneath.
                    FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                            itemHolder.expandArea.getLayoutParams();
                    expandParams.setMargins(0, -distance, 0, collapseHeight);
                    itemHolder.alarmItem.requestLayout();

                    // Set up the animator to animate the expansion.
                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
                            .setDuration(EXPAND_DURATION);
                    animator.setInterpolator(mExpandInterpolator);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            Float value = (Float) animator.getAnimatedValue();

                            // For each value from 0 to 1, animate the various parts of the layout.
                            itemHolder.alarmItem.getLayoutParams().height =
                                    (int) (value * distance + startingHeight);
                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(
                                    0, (int) -((1 - value) * distance), 0, collapseHeight);
                            itemHolder.arrow.setRotation(ROTATE_180_DEGREE * value);
                            itemHolder.summary.setAlpha(1 - value);
                            itemHolder.hairLine.setAlpha(1 - value);

                            itemHolder.alarmItem.requestLayout();
                        }
                    });
                    // Set everything to their final values when the animation's done.
                    animator.addListener(new AnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Set it back to wrap content since we'd explicitly set the height.
                            itemHolder.alarmItem.getLayoutParams().height =
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

        private boolean isAlarmExpanded(Alarm alarm) {
            return mExpandedId == alarm.id;
        }

        private void collapseAlarm(final ItemHolder itemHolder, boolean animate) {
            mExpandedId = AlarmClockFragment.INVALID_ID;
            mExpandedItemHolder = null;

            // Save the starting height so we can animate from this value.
            final int startingHeight = itemHolder.alarmItem.getHeight();

            // Set the expand area to gone so we can measure the height to animate to.
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, false /* expanded */);
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
            final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }

                    // Calculate some values to help with the animation.
                    final int endingHeight = itemHolder.alarmItem.getHeight();
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
                            itemHolder.alarmItem.getLayoutParams().height =
                                    (int) (value * distance + startingHeight);
                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(
                                    0, (int) (value * distance), 0, mCollapseExpandHeight);
                            itemHolder.arrow.setRotation(ROTATE_180_DEGREE * (1 - value));
                            itemHolder.delete.setAlpha(value);
                            itemHolder.summary.setAlpha(value);
                            itemHolder.hairLine.setAlpha(value);

                            itemHolder.alarmItem.requestLayout();
                        }
                    });
                    animator.setInterpolator(mCollapseInterpolator);
                    // Set everything to their final values when the animation's done.
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Set it back to wrap content since we'd explicitly set the height.
                            itemHolder.alarmItem.getLayoutParams().height =
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
                    if (h != null && h.alarm.id == id) {
                        return v;
                    }
                }
            }
            return null;
        }

        public long getExpandedId() {
            return mExpandedId;
        }

        public long[] getSelectedAlarmsArray() {
            int index = 0;
            long[] ids = new long[mSelectedAlarms.size()];
            for (long id : mSelectedAlarms) {
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
}
