package com.stripe.android.view;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

class ErrorListener implements StripeEditText.ErrorMessageListener {

    TextInputLayout textInputLayout;

    ErrorListener(TextInputLayout textInputLayout) {
        this.textInputLayout = textInputLayout;
    }

    @Override
    public void displayErrorMessage(@Nullable String message) {
        if (message == null) {
            textInputLayout.setErrorEnabled(false);
        } else {
            textInputLayout.setError(message);
        }
    }
}
