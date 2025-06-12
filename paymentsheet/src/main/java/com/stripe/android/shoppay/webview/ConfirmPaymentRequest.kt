package com.stripe.android.shoppay.webview

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Represents a confirm payment request from the web view.
 */
@Parcelize
data class ConfirmPaymentRequest(
    val requestId: String,
    val paymentDetails: PaymentDetails,
    val timestamp: Long
) : StripeModel {
    /**
     * Represents payment details within a confirm payment request.
     */
    @Parcelize
    data class PaymentDetails(
        val billingDetails: BillingDetails,
        val shippingAddress: ShippingAddress,
        val shippingRate: ShippingRate,
        val mode: String,
        val captureMethod: String,
        val paymentMethod: String?,
        val createPaymentMethodEnabled: Boolean,
        val amount: Long
    ) : StripeModel

    /**
     * Represents billing details within payment details.
     */
    @Parcelize
    data class BillingDetails(
        val address: Address,
        val email: String,
        val name: String,
        val phone: String
    ) : StripeModel

    /**
     * Represents an address within billing or shipping details.
     */
    @Parcelize
    data class Address(
        val city: String,
        val country: String,
        val line1: String,
        val postalCode: String,
        val state: String
    ) : StripeModel

    /**
     * Represents shipping address within payment details.
     */
    @Parcelize
    data class ShippingAddress(
        val city: String,
        val country: String,
        val line: List<String>,
        val postalCode: String,
        val state: String
    ) : StripeModel

    /**
     * Represents shipping rate within payment details.
     */
    @Parcelize
    data class ShippingRate(
        val id: String,
        val displayName: String,
        val amount: Long
    ) : StripeModel
}
