package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
class KeyboardController(
    private val activity: Activity
) {
    private val inputMethodManager: InputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    /**
     * Hide virtual keyboard
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // For paymentsheet
    @JvmSynthetic
    fun hide() {
        if (inputMethodManager.isAcceptingText) {
            inputMethodManager.hideSoftInputFromWindow(
                activity.currentFocus?.windowToken,
                0
            )
        }
    }
}
