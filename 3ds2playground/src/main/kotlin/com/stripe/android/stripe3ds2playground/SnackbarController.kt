package com.stripe.android.stripe3ds2playground

import android.annotation.SuppressLint
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

internal class SnackbarController internal constructor(
    private val view: CoordinatorLayout
) {
    @SuppressLint("WrongConstant")
    fun show(message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .show()
    }
}
