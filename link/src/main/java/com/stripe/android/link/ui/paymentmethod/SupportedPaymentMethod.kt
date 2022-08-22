package com.stripe.android.link.ui.paymentmethod

import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.elements.FormItemSpec
import com.stripe.android.ui.core.forms.LinkCardForm

/**
 * Represents the Payment Methods that are supported by Link.
 *
 * @param type The Payment Method type. Matches the [ConsumerPaymentDetails] types.
 * @param formSpec Specification of how the payment method data collection UI should look.
 */
internal enum class SupportedPaymentMethod(
    val type: String,
    val formSpec: List<FormItemSpec>
) {
    Card(
        ConsumerPaymentDetails.Card.type,
        LinkCardForm.items
    ) {
        override fun createParams(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            email: String
        ) = ConsumerPaymentDetailsCreateParams.Card(
            paymentMethodCreateParams.toParamMap(),
            email
        )

        /**
         * CVC is not passed during creation, and must be included when confirming the payment.
         */
        override fun extraConfirmationParams(paymentMethodCreateParams: PaymentMethodCreateParams) =
            (paymentMethodCreateParams.toParamMap()["card"] as? Map<*, *>)?.let { card ->
                mapOf("card" to mapOf("cvc" to card["cvc"]))
            }
    },
    BankAccount(
        ConsumerPaymentDetails.BankAccount.type,
        emptyList()
    ) {
        override fun createParams(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            email: String
        ): ConsumerPaymentDetailsCreateParams {
            TODO("Not yet implemented")
        }
    };

    /**
     * Build the [ConsumerPaymentDetailsCreateParams] that will to create this payment method.
     */
    abstract fun createParams(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        email: String
    ): ConsumerPaymentDetailsCreateParams

    /**
     * A map containing additional parameters that must be sent during payment confirmation.
     */
    open fun extraConfirmationParams(paymentMethodCreateParams: PaymentMethodCreateParams):
        Map<String, Any>? = null

    internal companion object {
        val allTypes = setOf(Card.type, BankAccount.type)
    }
}
