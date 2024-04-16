package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface CreateFinancialConnectionsSessionParams {
    fun toMap(): Map<String, Any>

    data class InstantDebits(
        val clientSecret: String,
        val customerEmailAddress: String?
    ): CreateFinancialConnectionsSessionParams {
        override fun toMap(): Map<String, Any> {
            return mapOf(
                PARAM_CLIENT_SECRET to clientSecret,
                "hosted_surface" to "payment_element",
                "product" to "instant_debits",
                "attach_required" to true,
                "payment_method_data[type]" to "link"
            )
        }
    }

    data class USBankAccount(
        val clientSecret: String,
        val customerName: String,
        val customerEmailAddress: String?
    ): CreateFinancialConnectionsSessionParams {
        override fun toMap(): Map<String, Any> {
            val paymentMethod = PaymentMethodCreateParams.createUSBankAccount(
                billingDetails = PaymentMethod.BillingDetails(
                    name = customerName,
                    email = customerEmailAddress
                )
            )
            return mapOf(
                PARAM_CLIENT_SECRET to clientSecret,
                PARAM_PAYMENT_METHOD_DATA to paymentMethod.toParamMap()
            )
        }
    }

    private companion object {
        const val PARAM_CLIENT_SECRET = "client_secret"
        const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
    }
}
