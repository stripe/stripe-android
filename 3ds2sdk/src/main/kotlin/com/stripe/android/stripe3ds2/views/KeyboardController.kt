package com.stripe.android.stripe3ds2.views

import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity

internal class KeyboardController(
    private val activity: FragmentActivity
) {
    fun hide() {
        activity.getSystemService<InputMethodManager>()?.let { inputMethodManager ->
            if (inputMethodManager.isAcceptingText) {
                inputMethodManager.hideSoftInputFromWindow(
                    activity.currentFocus?.windowToken,
                    0
                )
            }
        }
    }
}
