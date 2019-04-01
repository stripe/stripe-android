package com.stripe.android.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

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
