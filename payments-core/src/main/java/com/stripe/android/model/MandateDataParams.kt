package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Mandate Data parameters for
 * [confirming a PaymentIntent](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate_data)
 * or [confirming a SetupIntent](https://stripe.com/docs/api/setup_intents/confirm#confirm_setup_intent-mandate_data)
 */
@Parcelize
data class MandateDataParams constructor(
    private val type: Type
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_CUSTOMER_ACCEPTANCE to mapOf(
                PARAM_TYPE to type.code,
                type.code to type.toParamMap()
            )
        )
    }

    sealed class Type(
        internal val code: String
    ) : StripeParamsModel, Parcelable {

        /**
         * If this is a Mandate accepted online, this hash contains details about the online acceptance.
         *
         * [mandate_data.customer_acceptance.online](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate_data-customer_acceptance-online)
         */
        @Parcelize
        data class Online internal constructor(
            /**
             * The IP address from which the Mandate was accepted by the customer.
             *
             * [mandate_data.customer_acceptance.online.ip_address](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate_data-customer_acceptance-online-ip_address)
             */
            private val ipAddress: String? = null,

            /**
             * The user agent of the browser from which the Mandate was accepted by the customer.
             *
             * [mandate_data.customer_acceptance.online.user_agent](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate_data-customer_acceptance-online-user_agent)
             */
            private val userAgent: String? = null,

            private val inferFromClient: Boolean = false
        ) : Type("online") {

            constructor(
                /**
                 * The IP address from which the Mandate was accepted by the customer.
                 *
                 * [mandate_data.customer_acceptance.online.ip_address](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate_data-customer_acceptance-online-ip_address)
                 */
                ipAddress: String,

                /**
                 * The user agent of the browser from which the Mandate was accepted by the customer.
                 *
                 * [mandate_data.customer_acceptance.online.user_agent](https://stripe.com/docs/api/payment_intents/confirm#confirm_payment_intent-mandate_data-customer_acceptance-online-user_agent)
                 */
                userAgent: String
            ) : this(
                ipAddress,
                userAgent,
                inferFromClient = false
            )

            override fun toParamMap(): Map<String, Any> {
                return if (inferFromClient) {
                    mapOf(PARAM_INFER_FROM_CLIENT to true)
                } else {
                    mapOf(
                        PARAM_IP_ADDRESS to ipAddress.orEmpty(),
                        PARAM_USER_AGENT to userAgent.orEmpty()
                    )
                }
            }

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
            companion object {
                private const val PARAM_IP_ADDRESS = "ip_address"
                private const val PARAM_USER_AGENT = "user_agent"
                private const val PARAM_INFER_FROM_CLIENT = "infer_from_client"

                val DEFAULT = Online(inferFromClient = true)
            }
        }
    }

    private companion object {
        private const val PARAM_CUSTOMER_ACCEPTANCE = "customer_acceptance"
        private const val PARAM_TYPE = "type"
    }
}
