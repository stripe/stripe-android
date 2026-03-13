package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.ClientAttributionMetadata

/**
 * Parameters for confirming a checkout session via the confirm API
 * (`/v1/payment_pages/{cs_id}/confirm`).
 */
internal data class ConfirmCheckoutSessionParams(
    private val paymentMethodId: String,
    private val clientAttributionMetadata: ClientAttributionMetadata,
    private val returnUrl: String,
    private val savePaymentMethod: Boolean?,
    private val expectedAmount: Long? = null,
) {
    fun toParamMap(): Map<String, Any> {
        return buildMap {
            put("payment_method", paymentMethodId)
            put("client_attribution_metadata", clientAttributionMetadata.toParamMap())
            put("return_url", returnUrl)
            if (savePaymentMethod != null) {
                put("save_payment_method", savePaymentMethod)
            }
            if (expectedAmount != null) {
                put("expected_amount", expectedAmount)
            }
        }
    }
}
