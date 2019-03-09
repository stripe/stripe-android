package com.stripe.example.controller;

import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.stripe.example.R;
import com.stripe.example.dialog.ErrorDialogFragment;

/**
 * A convenience class to handle displaying error dialogs.
 */
public class ErrorDialogHandler {

    @NonNull private final FragmentManager mFragmentManager;

    public ErrorDialogHandler(@NonNull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    public void showError(@NonNull String errorMessage) {
        DialogFragment fragment = ErrorDialogFragment.newInstance(
                R.string.validationErrors, errorMessage);
        fragment.show(mFragmentManager, "error");
    }
}
