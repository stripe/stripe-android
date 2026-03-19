package com.stripe.android.stripecardscan.scanui.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat

/**
 * Set a view's visibility.
 */
internal fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * Make a view visible.
 */
internal fun View.show() = setVisible(true)

/**
 * Make a view invisible.
 */
internal fun View.hide() = setVisible(false)

/**
 * Get a [ColorInt] from a [ColorRes].
 */
@ColorInt
internal fun Context.getColorByRes(@ColorRes colorRes: Int) =
    ContextCompat.getColor(this, colorRes)

/**
 * This is copied from Resources.java for API 29 so that we can continue to support API 21.
 */
internal fun Context.getFloatResource(@DimenRes id: Int): Float {
    val value = TypedValue()
    resources.getValue(id, value, true)
    if (value.type == TypedValue.TYPE_FLOAT) {
        return value.float
    }
    throw NotFoundException(
        "Resource ID #0x${Integer.toHexString(id)} type " +
            "#0x${Integer.toHexString(value.type)} is not valid"
    )
}
