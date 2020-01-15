package com.stripe.example.activity

import android.annotation.SuppressLint
import android.view.View
import com.google.android.material.snackbar.Snackbar

internal class SnackbarController internal constructor(val view: View) {
    @SuppressLint("WrongConstant")
    fun show(message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .show()
    }
}
