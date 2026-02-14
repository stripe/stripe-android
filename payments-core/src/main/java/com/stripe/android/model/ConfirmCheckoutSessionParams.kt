package com.stripe.android.model

import androidx.annotation.RestrictTo

/**
 * Parameters for confirming a checkout session via the confirm API
 * (`/v1/payment_pages/{cs_id}/confirm`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConfirmCheckoutSessionParams(
    val checkoutSessionId: String,
    val paymentMethodId: String,
    val clientAttributionMetadata: ClientAttributionMetadata,
    val returnUrl: String,
    val savePaymentMethod: Boolean?,
) {
    fun toParamMap(): Map<String, Any> {
        return buildMap {
            put("payment_method", paymentMethodId)
            put("client_attribution_metadata", clientAttributionMetadata.toParamMap())
            put("return_url", returnUrl)
            if (savePaymentMethod == true) {
                put("save_payment_method", true)
            }
        }
    }
}
