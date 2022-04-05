package com.stripe.android.camera.scanui.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.core.content.ContextCompat

/**
 * Get a [Drawable] from a [DrawableRes]
 */
internal fun Context.getDrawableByRes(@DrawableRes drawableRes: Int) =
    ContextCompat.getDrawable(this, drawableRes)

/**
 * Get a rect from a view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun View.asRect() = Rect(left, top, right, bottom)

/**
 * Set the image of an [ImageView] using a [DrawableRes].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ImageView.setDrawable(@DrawableRes drawableRes: Int) {
    this.setImageDrawable(this.context.getDrawableByRes(drawableRes))
}

/**
 * Set the image of an [ImageView] using a [DrawableRes] and start the animation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ImageView.startAnimation(@DrawableRes drawableRes: Int) {
    val d = this.context.getDrawableByRes(drawableRes)
    setImageDrawable(d)
    if (d is Animatable) {
        d.start()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ImageView.startAnimationIfNotRunning(@DrawableRes drawableRes: Int) {
    drawable.let { currentDrawable ->
        // if is not Animatable, it's possible that the ImageView is just initialized from XML
        if (currentDrawable !is Animatable || !currentDrawable.isRunning) {
            startAnimation(drawableRes)
        }
    }
}

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
