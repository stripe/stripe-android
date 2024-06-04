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
            params[LINK_PARAM_BILLING_EMAIL_ADDRESS] = email

            ConsumerPaymentDetails.Card.getAddressFromMap(cardPaymentMethodCreateParamsMap)?.let {
                params.plusAssign(it)
            }

            // only card number, exp_month and exp_year are included
            (cardPaymentMethodCreateParamsMap[BASE_PARAM_CARD] as? Map<*, *>)?.let { createParamsMap ->
                params[LINK_PARAM_CARD] = createParamsMap.filterKeys { key ->
                    key in setOf(BASE_PARAM_CARD_NUMBER, BASE_PARAM_CARD_EXPIRY_MONTH, BASE_PARAM_CARD_EXPIRY_YEAR)
                }.toMutableMap().apply {
                    val networks = createParamsMap[BASE_PARAM_NETWORKS] as? Map<*, *>
                    val preferredNetwork = networks?.get(BASE_PARAM_PREFERRED) as? String

                    preferredNetwork?.let { network ->
                        put(LINK_PARAM_PREFERRED_NETWORK, network)
                    }
                }.toMap()
            }

            return params
        }

        companion object {
            private const val BASE_PARAM_CARD = "card"
            private const val BASE_PARAM_CARD_NUMBER = "number"
            private const val BASE_PARAM_CARD_EXPIRY_MONTH = "exp_month"
            private const val BASE_PARAM_CARD_EXPIRY_YEAR = "exp_year"
            private const val BASE_PARAM_NETWORKS = "networks"
            private const val BASE_PARAM_PREFERRED = "preferred"

            private const val LINK_PARAM_CARD = "card"
            private const val LINK_PARAM_BILLING_EMAIL_ADDRESS = "billing_email_address"
            private const val LINK_PARAM_PREFERRED_NETWORK = "preferred_network"

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
