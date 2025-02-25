package com.stripe.android.connect.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View

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

/**
 * Returns the nearest ancestor [View], or null if not found.
 */
internal fun View.findNearestAncestorView(): View? {
    var parent = this.parent
    while (parent != null) {
        when (val v = parent) {
            is View -> return v
            else -> parent = v.parent
        }
    }
    return null
}

/**
 * Returns an [Activity] associated with this View or one of its ancestors, or null if not found.
 */
internal fun View.findActivity(): Activity? {
    var currentView: View? = this
    while (currentView != null) {
        val activity = currentView.context.findActivity()
        if (activity != null) {
            return activity
        }
        currentView = currentView.findNearestAncestorView()
    }
    return null
}
