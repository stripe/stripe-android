package com.stripe.android.model

internal data class PaymentIntentLinkAccountSessionParams(
    val paymentIntentClientSecret: String,
    val customerName: String,
    val customerEmailAddress: String?,
) {
    fun toMap(): Map<String, Any> = toMap(
        paymentIntentClientSecret,
        customerName,
        customerEmailAddress
    )
}

internal data class SetupIntentLinkAccountSessionParams(
    val setupIntentClientSecret: String,
    val customerName: String,
    val customerEmailAddress: String?,
) {
    fun toMap(): Map<String, Any> = toMap(
        setupIntentClientSecret,
        customerName,
        customerEmailAddress
    )
}

private fun toMap(
    clientSecret: String,
    customerName: String,
    customerEmailAddress: String?,
): Map<String, Any> {
    val paymentMethod = PaymentMethodCreateParams(
        type = PaymentMethod.Type.USBankAccount,
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

private const val PARAM_CLIENT_SECRET = "client_secret"
private const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
