package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.PaymentMethod

/**
 * Parameters for updating a saved card on a checkout session via the update API
 * (`/v1/payment_pages/{cs_id}` with `payment_method_to_update`).
 *
 * Only card expiry and billing details can be updated; card brand and other card fields are not
 * supported by the endpoint.
 */
internal data class CheckoutSessionUpdatePaymentMethodParams(
    private val paymentMethodId: String,
    private val expiryMonth: Int?,
    private val expiryYear: Int?,
    private val billingDetails: PaymentMethod.BillingDetails?,
) {
    val hasSupportedUpdates: Boolean
        get() = billingDetails != null || (expiryMonth != null && expiryYear != null)

    fun toParamMap(): Map<String, Any> {
        // Nested maps are flattened to `key[sub][sub]` form by the networking layer.
        return buildMap {
            put("payment_method_to_update[payment_method_id]", paymentMethodId)
            billingDetails?.let {
                put("payment_method_to_update[billing_details]", it.toParamMap())
            }
            if (expiryMonth != null && expiryYear != null) {
                put(
                    "payment_method_to_update[expiry_details]",
                    mapOf("exp_month" to expiryMonth, "exp_year" to expiryYear),
                )
            }
            put("elements_session_client[is_aggregation_expected]", "true")
        }
    }
}
