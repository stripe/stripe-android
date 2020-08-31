package com.stripe.example.activity

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager

internal class KeyboardController(
    private val activity: Activity
) {
    private val inputMethodManager: InputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    /**
     * Hide virtual keyboard
     */
    internal fun hide() {
        if (inputMethodManager.isAcceptingText) {
            inputMethodManager.hideSoftInputFromWindow(
                activity.currentFocus?.windowToken,
                0
            )
        }
    }
}
