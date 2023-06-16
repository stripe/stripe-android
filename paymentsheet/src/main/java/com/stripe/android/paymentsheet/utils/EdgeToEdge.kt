package com.stripe.android.paymentsheet.utils

import android.view.View
import android.view.WindowInsets
import androidx.core.view.doOnAttach

// Taken from https://medium.com/androiddevelopers/windowinsets-listeners-to-layouts-8f9ccc8fa4d1
internal fun View.doOnApplyWindowInsets(f: (View, WindowInsets, InitialPadding) -> Unit) {
    val initialPadding = InitialPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    setOnApplyWindowInsetsListener { v, insets ->
        f(v, insets, initialPadding)
        insets
    }
    requestApplyInsetsWhenAttached()
}

internal data class InitialPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal fun View.requestApplyInsetsWhenAttached() {
    doOnAttach {
        it.requestApplyInsets()
    }
}
