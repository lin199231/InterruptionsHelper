package com.mk.interruptionshelper;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.PopupMenuCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;

/**
 * Created by MK on 2015/7/20.
 */
public class InterruptionsHelperFragment extends Fragment {

    protected ImageButton mFab;
    protected ImageButton mLeftButton;
    protected ImageButton mRightButton;

    public void onPageChanged(int page) {
        // Do nothing here , only in derived classes
    }

    public void onFabClick(View view){
        // Do nothing here , only in derived classes
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getActivity();
        if (activity instanceof InterruptionsHelper) {
            final InterruptionsHelper interruptionsHelperActivity = (InterruptionsHelper) activity;
            mFab = interruptionsHelperActivity.getFab();
            mLeftButton = interruptionsHelperActivity.getLeftButton();
            mRightButton = interruptionsHelperActivity.getRightButton();
        }
    }

    public void setFabAppearance() {
        // Do nothing here , only in derived classes
    }

    public void setLeftRightButtonAppearance() {
        // Do nothing here , only in derived classes
    }

    public void onLeftButtonClick(View view) {
        // Do nothing here , only in derived classes
    }

    public void onRightButtonClick(View view) {
        // Do nothing here , only in derived classes
    }
    /**
     * Installs click and touch listeners on a fake overflow menu button.
     *
     * @param menuButton the fragment's fake overflow menu button
     */
    public void setupFakeOverflowMenuButton(View menuButton) {
        final PopupMenu fakeOverflow = new PopupMenu(menuButton.getContext(), menuButton) {
            @Override
            public void show() {
                getActivity().onPrepareOptionsMenu(getMenu());
                super.show();
            }
        };
        fakeOverflow.inflate(R.menu.interruptions_helper_menu);
        fakeOverflow.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener () {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return getActivity().onOptionsItemSelected(item);
            }
        });

        menuButton.setOnTouchListener(PopupMenuCompat.getDragToOpenListener(fakeOverflow));
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fakeOverflow.show();
            }
        });
    }
}
