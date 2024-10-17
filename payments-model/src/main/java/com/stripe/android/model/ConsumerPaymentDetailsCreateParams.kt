package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface ConsumerPaymentDetailsCreateParams : StripeParamsModel, Parcelable {

    /**
     * Represents a new Card payment method that will be created using the
     * [cardPaymentMethodCreateParamsMap] values, converting from the `PaymentMethodCreateParams`
     * format to [ConsumerPaymentDetailsCreateParams] format.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Card(
        private val cardPaymentMethodCreateParamsMap: Map<String, @RawValue Any>,
        private val email: String,
        private val active: Boolean,
    ) : ConsumerPaymentDetailsCreateParams {

        override fun toParamMap(): Map<String, Any> {
            val params = mutableMapOf<String, Any>(
                "type" to "card",
                "active" to active,
                LINK_PARAM_BILLING_EMAIL_ADDRESS to email,
            )

            getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(cardPaymentMethodCreateParamsMap)?.let {
                params += it
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

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
            fun extraConfirmationParams(paymentMethodCreateParams: Map<String, Any>) =
                (paymentMethodCreateParams["card"] as? Map<*, *>)?.let { card ->
                    mapOf("card" to mapOf("cvc" to card["cvc"]))
                }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class BankAccount(
        private val bankAccountId: String,
        private val billingAddress: Map<String, @RawValue Any>?,
        private val billingEmailAddress: String,
    ) : ConsumerPaymentDetailsCreateParams {

        override fun toParamMap(): Map<String, Any> {
            val billingParams = buildMap {
                put("billing_email_address", billingEmailAddress)

                if (!billingAddress.isNullOrEmpty()) {
                    put("billing_address", billingAddress)
                }
            }

            val accountParams = mapOf(
                "type" to "bank_account",
                "bank_account" to mapOf(
                    "account" to bankAccountId,
                ),
            )

            return accountParams + billingParams
        }
    }
}

/**
 * Reads the address from the `PaymentMethodCreateParams` mapping format to the format
 * used by `ConsumerPaymentDetails`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun getConsumerPaymentDetailsAddressFromPaymentMethodCreateParams(
    cardPaymentMethodCreateParams: Map<String, Any>
): Pair<String, Any>? {
    val billingDetails = cardPaymentMethodCreateParams["billing_details"] as? Map<*, *>
    val address = billingDetails?.get("address") as? Map<*, *>
    return address?.let {
        // card["billing_details"]["address"] becomes card["billing_address"]
        "billing_address" to mapOf(
            // card["billing_details"]["address"]["country"]
            // becomes card["billing_address"]["country_code"]
            "country_code" to it["country"],
            "postal_code" to it["postal_code"]
        )
    }
}
