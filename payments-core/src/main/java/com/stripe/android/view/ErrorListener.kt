package com.stripe.android.view

import com.google.android.material.textfield.TextInputLayout

internal class ErrorListener(
    private val textInputLayout: TextInputLayout
) : StripeEditText.ErrorMessageListener {

    override fun displayErrorMessage(message: String?) {
        if (message == null) {
            textInputLayout.error = null
            textInputLayout.isErrorEnabled = false
        } else {
            textInputLayout.error = message
        }
    }
}
