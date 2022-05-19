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
        mapOf(PARAM_TYPE to type.code)

    companion object {
        private const val PARAM_TYPE = "type"
    }

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
            params[PARAM_BILLING_EMAIL_ADDRESS] = email

            // card["billing_details"]["address"] becomes card["billing_address"]
            (
                (cardPaymentMethodCreateParamsMap[PARAM_BILLING_DETAILS] as? Map<*, *>)
                    ?.get(PARAM_ADDRESS) as? Map<*, *>
                )?.let {
                params[PARAM_BILLING_ADDRESS] = mapOf(
                    // card["billing_details"]["address"]["country"]
                    // becomes card["billing_address"]["country_code"]
                    PARAM_COUNTRY_CODE to it[PARAM_COUNTRY],
                    PARAM_POSTAL_CODE to it[PARAM_POSTAL_CODE]
                )
            }

            // only card number, exp_month and exp_year are included
            (cardPaymentMethodCreateParamsMap[PARAM_CARD] as? Map<*, *>)?.let {
                params[PARAM_CARD] = it.toMutableMap().filterKeys { key ->
                    key in setOf(PARAM_CARD_NUMBER, PARAM_CARD_EXP_MONTH, PARAM_CARD_EXP_YEAR)
                }
            }
            return params
        }

        companion object {
            private const val PARAM_CARD = "card"
            private const val PARAM_CARD_NUMBER = "number"
            private const val PARAM_CARD_EXP_MONTH = "exp_month"
            private const val PARAM_CARD_EXP_YEAR = "exp_year"
            private const val PARAM_BILLING_EMAIL_ADDRESS = "billing_email_address"
            private const val PARAM_BILLING_ADDRESS = "billing_address"
            private const val PARAM_COUNTRY_CODE = "country_code"
            private const val PARAM_POSTAL_CODE = "postal_code"

            private const val PARAM_BILLING_DETAILS = "billing_details"
            private const val PARAM_ADDRESS = "address"
            private const val PARAM_COUNTRY = "country"
        }
    }
}
