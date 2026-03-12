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
) {
    fun toParamMap(): Map<String, Any> {
        return buildMap {
            put("payment_method", paymentMethodId)
            put("client_attribution_metadata", clientAttributionMetadata.toParamMap())
            put("return_url", returnUrl)
            if (savePaymentMethod != null) {
                put("save_payment_method", savePaymentMethod)
            }
            // Request full intent objects. Both are sent unconditionally (matching iOS behavior)
            // — the server ignores expands for the intent type that doesn't apply to the session mode.
            put(
                "expand",
                listOf(
                    "payment_intent",
                    "payment_intent.payment_method",
                    "setup_intent",
                    "setup_intent.payment_method",
                )
            )
        }
    }
}
