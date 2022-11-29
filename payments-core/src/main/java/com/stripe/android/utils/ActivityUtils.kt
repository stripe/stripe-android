package com.stripe.android.utils

import android.app.Activity

internal fun Activity.argsAreInvalid(argsProvider: () -> Unit): Boolean {
    return try {
        argsProvider()
        false
    } catch (e: IllegalArgumentException) {
        finish()
        true
    }
}
