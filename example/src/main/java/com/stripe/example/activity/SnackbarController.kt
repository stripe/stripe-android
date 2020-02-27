package com.stripe.example.activity

import android.annotation.SuppressLint
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

internal class SnackbarController internal constructor(val view: CoordinatorLayout) {
    @SuppressLint("WrongConstant")
    fun show(message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .show()
    }
}
