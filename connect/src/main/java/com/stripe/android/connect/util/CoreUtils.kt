package com.stripe.android.connect.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Returns the [Activity] that this [Context] is attached to, or null if none.
 */
internal fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}