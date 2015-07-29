package com.mk.interruptionhelper;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.mk.interruptionhelper.util.Utils;

/**
 * Settings for the InterruptionHelper
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        return super.onCreateOptionsMenu(menu);
    }
}