package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Model for Link Payment Method creation parameters, used for 'consumers/payment_details' endpoint.
 */
sealed class ConsumerPaymentDetailsCreateParams(
    internal val type: PaymentMethod.Type
) : StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> =
        mapOf("type" to type.code)

    /**
     * Represents a new Card payment method that will be created using the
     * [cardPaymentMethodCreateParamsMap] values, converting from the [PaymentMethodCreateParams]
     * format to [ConsumerPaymentDetailsCreateParams] format.
     */
    @Parcelize
    class Card(
        private val cardPaymentMethodCreateParamsMap: Map<String, @RawValue Any>,
        private val email: String
    ) : ConsumerPaymentDetailsCreateParams(PaymentMethod.Type.Card) {
        override fun toParamMap() = super.toParamMap()
            .plus(convertParamsMap())

        private fun convertParamsMap(): Map<String, Any> {
            val params: MutableMap<String, Any> = mutableMapOf()
            params["billing_email_address"] = email

            ConsumerPaymentDetails.Card.getAddressFromMap(cardPaymentMethodCreateParamsMap)?.let {
                params.plusAssign(it)
            }

            // only card number, exp_month and exp_year are included
            (cardPaymentMethodCreateParamsMap["card"] as? Map<*, *>)?.let {
                params["card"] = it.toMutableMap().filterKeys { key ->
                    key in setOf("number", "exp_month", "exp_year")
                }
            }
            return params
        }

        companion object {
            /**
             * A map containing additional parameters that must be sent during payment confirmation.
             * CVC is not passed during creation, and must be included when confirming the payment.
             */
            fun extraConfirmationParams(paymentMethodCreateParams: PaymentMethodCreateParams) =
                (paymentMethodCreateParams.toParamMap()["card"] as? Map<*, *>)?.let { card ->
                    mapOf("card" to mapOf("cvc" to card["cvc"]))
                }
        }
    }
}
