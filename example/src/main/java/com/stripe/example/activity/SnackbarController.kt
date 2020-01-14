package com.stripe.example.activity

import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

internal class SnackbarController internal constructor(val view: View) {
    fun show(message: String) {
        Snackbar.make(view, message, BaseTransientBottomBar.LENGTH_SHORT)
            .show()
    }
}
