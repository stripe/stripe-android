package com.stripe.android.model

internal object PaymentMethodCreateParamsFixtures {
    @JvmField
    val CARD = PaymentMethodCreateParams.Card.Builder()
        .setNumber("4242424242424242")
        .setExpiryMonth(1)
        .setExpiryYear(2024)
        .setCvc("111")
        .build()

    @JvmField
    val BILLING_DETAILS = PaymentMethod.BillingDetails.Builder()
        .setName("Home")
        .setEmail("me@example.com")
        .setPhone("1-800-555-1234")
        .setAddress(Address.Builder()
            .setLine1("123 Main St")
            .setCity("Los Angeles")
            .setState("CA")
            .setCountry("US")
            .build())
        .build()

    @JvmField
    val DEFAULT_CARD = PaymentMethodCreateParams.create(
        CARD,
        BILLING_DETAILS
    )

    @JvmField
    val DEFAULT_FPX = PaymentMethodCreateParams.create(
        PaymentMethodCreateParams.Fpx.Builder()
            .setBank("hsbc")
            .build()
    )

    @JvmStatic
    fun createWith(metadata: Map<String, String>): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.create(
            CARD,
            BILLING_DETAILS,
            metadata
        )
    }
}
