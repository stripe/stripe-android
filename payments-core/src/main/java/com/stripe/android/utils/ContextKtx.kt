@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.RestrictTo

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
