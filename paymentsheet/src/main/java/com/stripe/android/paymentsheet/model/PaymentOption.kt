package com.stripe.android.paymentsheet.model

import androidx.annotation.DrawableRes

/**
 * The customer's selected payment option.
 */
data class PaymentOption(
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    @DrawableRes val drawableResourceId: Int,

    /**
     * TODO: Add docs, maybe use JVM overloads and default to null?
     */
    val iconUrl: String?,

    /**
     * A label that describes the payment option.
     *
     * For example, "····4242" for a Visa ending in 4242.
     */
    val label: String
)
