package com.stripe.android.model

internal data class CreateFinancialConnectionsSessionParams(
    val clientSecret: String,
    val customerName: String,
    val customerEmailAddress: String?,
) {
    fun toMap(): Map<String, Any> {
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

    companion object {
        private const val PARAM_CLIENT_SECRET = "client_secret"
        private const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
    }
}
