package com.stripe.android.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;

import com.stripe.android.CustomerSession;
import com.stripe.android.R;
import com.stripe.android.exception.StripeException;

/**
 * Provides a toolbar, save button, and loading states for the save button.
 */
abstract class StripeActivity extends AppCompatActivity {

    BroadcastReceiver mAlertBroadcastReceiver;
    @Nullable AlertMessageListener mAlertMessageListener;
    boolean mCommunicating;
    Toolbar mToolbar;
    ProgressBar mProgressBar;
    ViewStub mViewStub;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stripe);
        mProgressBar = findViewById(R.id.progress_bar_as);
        mToolbar = findViewById(R.id.toolbar_as);
        mViewStub = findViewById(R.id.widget_viewstub_as);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setCommunicatingProgress(false);
        mAlertBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                StripeException exception = (StripeException)
                        intent.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
                showError(exception.getLocalizedMessage());
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAlertBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mAlertBroadcastReceiver,
                        new IntentFilter(CustomerSession.ACTION_API_EXCEPTION));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_source_menu, menu);
        menu.findItem(R.id.action_save).setEnabled(!mCommunicating);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            onActionSave();
            return true;
        } else {
            boolean handled = super.onOptionsItemSelected(item);
            if (!handled) {
                onBackPressed();
            }
            return handled;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.action_save);
        Drawable tintedIcon = ViewUtils.getTintedIconWithAttribute(
                this,
                getTheme(),
                R.attr.titleTextColor,
                R.drawable.ic_checkmark);
        saveItem.setIcon(tintedIcon);
        return super.onPrepareOptionsMenu(menu);
    }

    protected abstract void onActionSave();

    protected void setCommunicatingProgress(boolean communicating) {
        mCommunicating = communicating;
        if (communicating) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.GONE);
        }
        supportInvalidateOptionsMenu();
    }

    void setAlertMessageListener(@Nullable AlertMessageListener listener) {
        mAlertMessageListener = listener;
    }

    void showError(@NonNull String error) {
        if (mAlertMessageListener != null) {
            mAlertMessageListener.onAlertMessageDisplayed(error);
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setMessage(error)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }

    interface AlertMessageListener {
        void onAlertMessageDisplayed(String message);
    }
}
