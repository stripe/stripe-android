package com.stripe.example.controller;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.stripe.example.R;
import com.stripe.example.dialog.ErrorDialogFragment;

import java.lang.ref.WeakReference;

/**
 * A convenience class to handle displaying error dialogs.
 */
public class ErrorDialogHandler {

    @NonNull private final WeakReference<AppCompatActivity> mActivityRef;

    public ErrorDialogHandler(@NonNull AppCompatActivity activity) {
        mActivityRef = new WeakReference<>(activity);
    }

    public void show(@NonNull String errorMessage) {
        final AppCompatActivity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }

        ErrorDialogFragment.newInstance(activity.getString(R.string.validationErrors), errorMessage)
                .show(activity.getSupportFragmentManager(), "error");
    }
}
