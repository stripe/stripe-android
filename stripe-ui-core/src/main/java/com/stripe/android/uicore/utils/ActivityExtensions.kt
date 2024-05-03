package com.stripe.android.uicore.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo

/**
 * Extracts the [ComponentActivity] from the given [Context].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.extractActivity(): ComponentActivity? {
    var currentContext = this
    if (currentContext is ComponentActivity) {
        return currentContext
    } else {
        while (currentContext is ContextWrapper) {
            if (currentContext is ComponentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
    }
    return null
}
