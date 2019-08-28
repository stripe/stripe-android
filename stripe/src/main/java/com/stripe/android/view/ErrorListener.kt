package com.stripe.android.view

import android.support.design.widget.TextInputLayout

internal class ErrorListener(
    private val textInputLayout: TextInputLayout
) : StripeEditText.ErrorMessageListener {

    override fun displayErrorMessage(message: String?) {
        if (message == null) {
            textInputLayout.isErrorEnabled = false
        } else {
            textInputLayout.error = message
        }
    }
}
