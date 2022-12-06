package com.stripe.android.paymentsheet.model

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    internal var lightThemeIconUrl: String? = null
        private set

    internal var darkThemeIconUrl: String? = null
        private set

    private var imageLoader: suspend (PaymentOption) -> Drawable = {
        throw IllegalStateException("Must pass in an image loader to use iconDrawable.")
    }

    internal constructor(
        @DrawableRes drawableResourceId: Int,
        label: String,
        lightThemeIconUrl: String?,
        darkThemeIconUrl: String?,
        imageLoader: suspend (PaymentOption) -> Drawable
    ) : this(drawableResourceId, label) {
        this.lightThemeIconUrl = lightThemeIconUrl
        this.darkThemeIconUrl = darkThemeIconUrl
        this.imageLoader = imageLoader
    }

    /**
     * Fetches the icon associated with this [PaymentOption].
     */
    suspend fun fetchIcon(): Drawable {
        return imageLoader(this)
    }

    /**
     * Fetches the icon associated with this [PaymentOption].
     *
     * Prefer the suspending variant.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun fetchIcon(onComplete: (Drawable) -> Unit) {
        GlobalScope.launch {
            onComplete(fetchIcon())
        }
    }
}
