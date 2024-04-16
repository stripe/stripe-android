package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface CreateFinancialConnectionsSessionParams {

    fun toMap(): Map<String, Any>

    data class USBankAccount(
        val clientSecret: String,
        val customerName: String,
        val customerEmailAddress: String?,
    ) : CreateFinancialConnectionsSessionParams {

        override fun toMap(): Map<String, Any> {
            val paymentMethod = PaymentMethodCreateParams.createUSBankAccount(
                billingDetails = PaymentMethod.BillingDetails(
                    name = customerName,
                    email = customerEmailAddress
                ),
            )

            return mapOf(
                PARAM_CLIENT_SECRET to clientSecret,
                PARAM_PAYMENT_METHOD_DATA to paymentMethod.toParamMap()
            )
        }
    }

    data class InstantDebits(
        val clientSecret: String,
        val customerEmailAddress: String?,
    ) : CreateFinancialConnectionsSessionParams {

        override fun toMap(): Map<String, Any> {
            val paymentMethod = PaymentMethodCreateParams(
                type = PaymentMethod.Type.Link,
                billingDetails = PaymentMethod.BillingDetails(
                    email = customerEmailAddress,
                ),
            )

            return mapOf(
                PARAM_CLIENT_SECRET to clientSecret,
                PARAM_PAYMENT_METHOD_DATA to paymentMethod.toParamMap(),
                PARAM_HOSTED_SURFACE to "payment_element",
                PARAM_PRODUCT to "instant_debits",
                PARAM_ATTACH_REQUIRED to true,
            )
        }
    }

    private companion object {
        const val PARAM_CLIENT_SECRET = "client_secret"
        const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
        const val PARAM_HOSTED_SURFACE = "hosted_surface"
        const val PARAM_PRODUCT = "product"
        const val PARAM_ATTACH_REQUIRED = "attach_required"
    }
}
