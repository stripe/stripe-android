package com.stripe.android.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;

class ErrorListener implements StripeEditText.ErrorMessageListener {

    @NonNull private final TextInputLayout mTextInputLayout;

    ErrorListener(@NonNull TextInputLayout textInputLayout) {
        mTextInputLayout = textInputLayout;
    }

    @Override
    public void displayErrorMessage(@Nullable String message) {
        if (message == null) {
            mTextInputLayout.setErrorEnabled(false);
        } else {
            mTextInputLayout.setError(message);
        }
    }
}
