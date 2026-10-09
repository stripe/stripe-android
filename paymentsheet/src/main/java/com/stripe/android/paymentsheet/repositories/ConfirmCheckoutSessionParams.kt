package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.Address
import com.stripe.android.model.ClientAttributionMetadata

/**
 * Parameters for confirming a checkout session via the confirm API
 * (`/v1/payment_pages/{cs_id}/confirm`).
 *
 * [expectedPaymentMethodType] identifies the PaymentMethod being confirmed and is omitted when its type is unavailable.
 * [expectedAmount] is the checkout session amount and is included for every confirmation, including zero.
 * [savePaymentMethod] is only applicable in payment/subscription mode and should be null for setup mode.
 */
internal data class ConfirmCheckoutSessionParams(
    private val paymentMethodId: String,
    private val expectedPaymentMethodType: String?,
    private val clientAttributionMetadata: ClientAttributionMetadata,
    private val returnUrl: String,
    private val expectedAmount: Long,
    private val savePaymentMethod: Boolean?,
    private val shipping: Shipping?,
    private val collectedInformation: CollectedInformation?,
) {
    fun toParamMap(): Map<String, Any> {
        return buildMap {
            put("payment_method", paymentMethodId)
            if (expectedPaymentMethodType != null) {
                put("expected_payment_method_type", expectedPaymentMethodType)
            }
            put("client_attribution_metadata", clientAttributionMetadata.toParamMap())
            put("return_url", returnUrl)
            put("expected_amount", expectedAmount)
            if (savePaymentMethod != null) {
                put("save_payment_method", savePaymentMethod)
            }
            if (collectedInformation != null) {
                put("collected_information", collectedInformation.toParamMap())
            }
            if (shipping != null) {
                put("shipping", shipping.toParamMap())
            }
        }
    }

    internal data class CollectedInformation(val email: String) {
        fun toParamMap(): Map<String, Any> = mapOf("email" to email)
    }

    internal data class Shipping(
        private val name: String?,
        private val address: Address?,
    ) {
        fun toParamMap(): Map<String, Any> {
            return buildMap {
                if (name != null) {
                    put("name", name)
                }
                if (address != null) {
                    put("address", address.toParamMap())
                }
            }
        }
    }
}
