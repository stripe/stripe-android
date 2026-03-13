package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.ClientAttributionMetadata

/**
 * Shared parameters for confirming a checkout session via the confirm API
 * (`/v1/payment_pages/{cs_id}/confirm`).
 *
 * Used directly for setup mode. For payment/subscription mode, see
 * [ConfirmCheckoutSessionPaymentParams] which adds payment-specific fields.
 */
internal open class ConfirmCheckoutSessionParams(
    private val paymentMethodId: String,
    private val clientAttributionMetadata: ClientAttributionMetadata,
    private val returnUrl: String,
) {
    open fun toParamMap(): Map<String, Any> {
        return buildMap {
            put("payment_method", paymentMethodId)
            put("client_attribution_metadata", clientAttributionMetadata.toParamMap())
            put("return_url", returnUrl)
        }
    }
}

/**
 * Payment/subscription-specific parameters for confirming a checkout session.
 *
 * Extends [ConfirmCheckoutSessionParams] with [expectedAmount] and [savePaymentMethod],
 * which are only applicable to payment and subscription modes.
 */
internal class ConfirmCheckoutSessionPaymentParams(
    paymentMethodId: String,
    clientAttributionMetadata: ClientAttributionMetadata,
    returnUrl: String,
    private val expectedAmount: Long,
    private val savePaymentMethod: Boolean?,
) : ConfirmCheckoutSessionParams(paymentMethodId, clientAttributionMetadata, returnUrl) {
    override fun toParamMap(): Map<String, Any> {
        return buildMap {
            putAll(super.toParamMap())
            put("expected_amount", expectedAmount)
            if (savePaymentMethod != null) {
                put("save_payment_method", savePaymentMethod)
            }
        }
    }
}
