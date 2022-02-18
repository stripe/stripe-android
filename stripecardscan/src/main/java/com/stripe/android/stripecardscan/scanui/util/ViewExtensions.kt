package com.stripe.android.stripecardscan.scanui.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.PointF
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

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
 * Get a [Drawable] from a [DrawableRes]
 */
internal fun Context.getDrawableByRes(@DrawableRes drawableRes: Int) =
    ContextCompat.getDrawable(this, drawableRes)

/**
 * Convert an int in DP to pixels.
 */
internal fun Context.dpToPixels(dp: Int) = (dp * resources.displayMetrics.density).roundToInt()

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

/**
 * Set the size of a text field using a dimension.
 */
internal fun TextView.setTextSizeByRes(@DimenRes id: Int) {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, this.resources.getDimension(id))
}

/**
 * Determine the center point of a view.
 */
internal fun View.centerPoint() = PointF(left + width / 2F, top + height / 2F)
