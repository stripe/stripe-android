package com.stripe.android.paymentsheet.model

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

/**
 * The customer's selected payment option.
 */
data class PaymentOption @Deprecated("Not intended for public use.") constructor(
    @Deprecated("Please use iconDrawable instead.")
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    @DrawableRes val drawableResourceId: Int,

    /**
     * A label that describes the payment option.
     *
     * For example, "····4242" for a Visa ending in 4242.
     */
    val label: String
) {
    internal var iconUrl: String? = null
        private set

    private var imageLoader: suspend (PaymentOption) -> Drawable = {
        throw IllegalStateException("Must pass in an image loader to use iconDrawable.")
    }

    internal constructor(
        @DrawableRes drawableResourceId: Int,
        label: String,
        iconUrl: String?,
        imageLoader: suspend (PaymentOption) -> Drawable
    ) : this(drawableResourceId, label) {
        this.iconUrl = iconUrl
        this.imageLoader = imageLoader
    }

    suspend fun iconDrawable(): Drawable {
        return imageLoader(this)
    }
}
