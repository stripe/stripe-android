package com.stripe.android.view;

import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;

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
