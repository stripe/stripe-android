package com.stripe.android.paymentsheet.model

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable

/**
 * The customer's selected payment option.
 */
data class PaymentOption
@Deprecated("Not intended for public use.")
constructor(
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    @Deprecated("Please use fetchIcon instead.")
    @DrawableRes
    val drawableResourceId: Int,

    /**
     * A label that describes the payment option.
     *
     * For example, "····4242" for a Visa ending in 4242.
     */
    val label: String
) {
    // These aren't part of the primary constructor in order to maintain binary compatibility.
    private var lightThemeIcon: Drawable? = null
    private var darkThemeIcon: Drawable? = null
    private lateinit var resources: Resources

    @Suppress("DEPRECATION")
    internal constructor(
        resources: Resources,
        @DrawableRes drawableResourceId: Int,
        label: String,
        lightThemeIcon: Bitmap? = null,
        darkThemeIcon: Bitmap? = null,
    ) : this(drawableResourceId, label) {
        this.lightThemeIcon = lightThemeIcon?.toDrawable(resources)
        this.darkThemeIcon = darkThemeIcon?.toDrawable(resources)
        this.resources = resources
    }

    /**
     * Fetches the icon associated with this [PaymentOption].
     */
    fun icon(): Drawable {
        val darkThemeIcon = darkThemeIcon
        val lightThemeIcon = lightThemeIcon
        return if (isDarkTheme() && darkThemeIcon != null) {
            darkThemeIcon
        } else if (lightThemeIcon != null) {
            lightThemeIcon
        } else {
            @Suppress("DEPRECATION")
            ResourcesCompat.getDrawable(
                resources,
                drawableResourceId,
                null
            ) ?: ShapeDrawable()
        }
    }

    private fun isDarkTheme(): Boolean {
        return resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
