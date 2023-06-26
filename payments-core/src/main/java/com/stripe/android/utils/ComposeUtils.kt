@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberActivity(
    errorMessage: () -> String,
): Activity {
    val context = LocalContext.current
    return remember(context) {
        requireNotNull(context.findActivity(), errorMessage)
    }
}

@Composable
fun rememberActivityOrNull(): Activity? {
    val context = LocalContext.current
    return remember(context) {
        context.findActivity()
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
