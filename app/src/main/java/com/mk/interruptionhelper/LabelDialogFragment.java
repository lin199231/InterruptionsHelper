package com.mk.interruptionhelper;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.mk.interruptionhelper.provider.Interruption;
import com.mk.interruptionhelper.timer.TimerObj;
import com.mk.interruptionhelper.util.LogUtils;

/**
 * Created by dhdev_000 on 2015/7/30.
 */
/**
 * DialogFragment to edit label.
 */
public class LabelDialogFragment extends DialogFragment {

    private static final String KEY_LABEL = "label";
    private static final String KEY_INTERRUPTION = "interruption";
    private static final String KEY_TIMER = "timer";
    private static final String KEY_TAG = "tag";

    private EditText mLabelBox;

    public static LabelDialogFragment newInstance(Interruption interruption, String label, String tag) {
        final LabelDialogFragment frag = new LabelDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_LABEL, label);
        args.putParcelable(KEY_INTERRUPTION, interruption);
        args.putString(KEY_TAG, tag);
        frag.setArguments(args);
        return frag;
    }

    public static LabelDialogFragment newInstance(TimerObj timer, String label, String tag) {
        final LabelDialogFragment frag = new LabelDialogFragment();
        Bundle args = new Bundle();
        args.putString(KEY_LABEL, label);
        args.putParcelable(KEY_TIMER, timer);
        args.putString(KEY_TAG, tag);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        final String label = bundle.getString(KEY_LABEL);
        final Interruption interruption = bundle.getParcelable(KEY_INTERRUPTION);
        final TimerObj timer = bundle.getParcelable(KEY_TIMER);
        final String tag = bundle.getString(KEY_TAG);

        final View view = inflater.inflate(R.layout.label_dialog, container, false);

        mLabelBox = (EditText) view.findViewById(R.id.labelBox);
        mLabelBox.setText(label);
        mLabelBox.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    set(interruption, timer, tag);
                    return true;
                }
                return false;
            }
        });
        mLabelBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setLabelBoxBackground(s == null || TextUtils.isEmpty(s));
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        setLabelBoxBackground(TextUtils.isEmpty(label));

        final Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        final Button setButton = (Button) view.findViewById(R.id.setButton);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                set(interruption, timer, tag);
            }
        });

        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return view;
    }

    private void set(Interruption interruption, TimerObj timer, String tag) {
        String label = mLabelBox.getText().toString();
        if (label.trim().length() == 0) {
            // Don't allow user to input label with only whitespace.
            label = "";
        }

        if (interruption != null) {
            set(interruption, tag, label);
        } else if (timer != null) {
            set(timer, tag, label);
        } else {
            LogUtils.e("No alarm or timer available.");
        }
    }

    private void set(Interruption interruption, String tag, String label) {
        final Activity activity = getActivity();
        // TODO just pass in a listener in newInstance()
        if (activity instanceof AlarmLabelDialogHandler) {
            ((InterruptionHelper) getActivity()).onDialogLabelSet(interruption, label, tag);
        } else {
            LogUtils.e("Error! Activities that use LabelDialogFragment must implement "
                    + "AlarmLabelDialogHandler");
        }
        dismiss();
    }

    private void set(TimerObj timer, String tag, String label) {
        final Activity activity = getActivity();
        // TODO just pass in a listener in newInstance()
        if (activity instanceof TimerLabelDialogHandler){
            ((InterruptionHelper) getActivity()).onDialogLabelSet(timer, label, tag);
        } else {
            LogUtils.e("Error! Activities that use LabelDialogFragment must implement "
                    + "AlarmLabelDialogHandler or TimerLabelDialogHandler");
        }
        dismiss();
    }

    private void setLabelBoxBackground(boolean emptyText) {
        mLabelBox.setBackgroundResource(emptyText ?
                R.drawable.bg_edittext_default : R.drawable.bg_edittext_activated);
    }

    interface AlarmLabelDialogHandler {
        void onDialogLabelSet(Interruption interruption, String label, String tag);
    }

    interface TimerLabelDialogHandler {
        void onDialogLabelSet(TimerObj timer, String label, String tag);
    }
}

