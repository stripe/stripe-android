package com.stripe.android.model

internal data class CreateLinkAccountSessionParams(
    /**
     * client secret of a PaymentIntent / SetupIntent.
     */
    val clientSecret: String,
    val customerName: String,
    val customerEmailAddress: String?,
) {
    fun toMap(): Map<String, Any> {
        val paymentMethod = PaymentMethodCreateParams.createUsBankAccount(
            PaymentMethod.BillingDetails(
                email = customerEmailAddress,
                name = customerName
            )
        )
        return mapOf(
            PARAM_CLIENT_SECRET to clientSecret,
            PARAM_PAYMENT_METHOD_DATA to paymentMethod.toParamMap()
        )
    }

    private companion object {
        const val PARAM_CLIENT_SECRET = "client_secret"
        const val PARAM_PAYMENT_METHOD_DATA = "payment_method_data"
    }
}
