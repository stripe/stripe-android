package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Information for displaying external payment methods (EPMs), delivered in the `v1/elements/sessions` response.
 *
 * See also: https://git.corp.stripe.com/stripe-internal/pay-server/blob/master/lib/elements/api/private/struct/external_payment_method_data.rb
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class ExternalPaymentMethodSpec(

    /**
     * The type of the external payment method, e.g. "external_foopay".
     *
     * These match the strings specified by the merchant in their configuration.
     * */
    @SerialName("type")
    val type: String,

    /** Localized label for the payment method, e.g. "FooPay". */
    @SerialName("label")
    val label: String,

    /**
     * URL of a 48x pixel tall, variable width PNG representing the payment method suitable for display against a light
     * background color.
     * */
    @SerialName("light_image_url")
    val lightImageUrl: String,

    /**
     * URL of a 48x pixel tall, variable width PNG representing the payment method suitable for display against a dark
     * background color. If null, use [lightImageUrl] instead.
     * */
    @SerialName("dark_image_url")
    val darkImageUrl: String? = null,
) : Parcelable
