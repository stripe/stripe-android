package com.stripe.android.model

internal object PaymentMethodCreateParamsFixtures {
    internal val CARD = PaymentMethodCreateParams.Card(
        number = "4242424242424242",
        expiryMonth = 1,
        expiryYear = 2024,
        cvc = "111"
    )

    internal val CARD_WITH_ATTRIBUTION = PaymentMethodCreateParams.Card(
        number = "4242424242424242",
        expiryMonth = 1,
        expiryYear = 2024,
        cvc = "111",
        attribution = setOf("CardMultilineWidget")
    )

    @JvmField
    internal val BILLING_DETAILS = PaymentMethod.BillingDetails(
        name = "Home",
        email = "me@example.com",
        phone = "1-800-555-1234",
        address = Address(
            line1 = "123 Main St",
            city = "Los Angeles",
            state = "CA",
            country = "US"
        )
    )

    @JvmField
    internal val DEFAULT_CARD = PaymentMethodCreateParams.create(
        CARD,
        BILLING_DETAILS
    )

    internal val DEFAULT_FPX = PaymentMethodCreateParams.create(
        PaymentMethodCreateParams.Fpx(
            bank = "hsbc"
        )
    )

    internal val DEFAULT_SEPA_DEBIT = PaymentMethodCreateParams.create(
        PaymentMethodCreateParams.SepaDebit(iban = "my_iban")
    )

    internal val AU_BECS_DEBIT = PaymentMethodCreateParams.create(
        PaymentMethodCreateParams.AuBecsDebit(
            bsbNumber = "000000",
            accountNumber = "000123456"
        ),
        BILLING_DETAILS
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
