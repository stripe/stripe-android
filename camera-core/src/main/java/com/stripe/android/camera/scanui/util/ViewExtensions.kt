package com.stripe.android.camera.scanui.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Get a [Drawable] from a [DrawableRes]
 */
internal fun Context.getDrawableByRes(@DrawableRes drawableRes: Int) =
    ContextCompat.getDrawable(this, drawableRes)

/**
 * Get a rect from a view.
 */
internal fun View.asRect() = Rect(left, top, right, bottom)

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
