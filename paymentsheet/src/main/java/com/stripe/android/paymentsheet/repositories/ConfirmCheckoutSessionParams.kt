package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.ClientAttributionMetadata

/**
 * Parameters for confirming a checkout session via the confirm API
 * (`/v1/payment_pages/{cs_id}/confirm`).
 *
 * [expectedAmount] and [savePaymentMethod] are only applicable in payment/subscription mode
 * and should be null for setup mode.
 */
internal data class ConfirmCheckoutSessionParams(
    private val paymentMethodId: String,
    private val clientAttributionMetadata: ClientAttributionMetadata,
    private val returnUrl: String,
    private val expectedAmount: Long? = null,
    private val savePaymentMethod: Boolean? = null,
) {
    fun toParamMap(): Map<String, Any> {
        return buildMap {
            put("payment_method", paymentMethodId)
            put("client_attribution_metadata", clientAttributionMetadata.toParamMap())
            put("return_url", returnUrl)
            if (expectedAmount != null) {
                put("expected_amount", expectedAmount)
            }
            if (savePaymentMethod != null) {
                put("save_payment_method", savePaymentMethod)
            }
        }
    }
}
