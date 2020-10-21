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
        name = "Jenny Rosen",
        email = "jenny.rosen@example.com",
        phone = "1-800-555-1234",
        address = Address(
            line1 = "1234 Main St",
            city = "San Francisco",
            state = "CA",
            country = "US",
            postalCode = "94111"
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

    internal val BACS_DEBIT = PaymentMethodCreateParams.create(
        bacsDebit = PaymentMethodCreateParams.BacsDebit(
            accountNumber = "00012345",
            sortCode = "108800"
        ),
        billingDetails = BILLING_DETAILS
    )

    internal val SOFORT = PaymentMethodCreateParams.create(
        sofort = PaymentMethodCreateParams.Sofort(
            country = "DE"
        ),
        billingDetails = BILLING_DETAILS
    )

    internal val P24 = PaymentMethodCreateParams.createP24(
        billingDetails = BILLING_DETAILS
    )

    internal val BANCONTACT = PaymentMethodCreateParams.createBancontact(
        billingDetails = BILLING_DETAILS
    )

    internal val GIROPAY = PaymentMethodCreateParams.createGiropay(
        billingDetails = BILLING_DETAILS
    )

    internal val EPS = PaymentMethodCreateParams.createEps(
        billingDetails = BILLING_DETAILS
    )

    internal val GRABPAY = PaymentMethodCreateParams.createGrabPay(
        billingDetails = BILLING_DETAILS
    )

    internal val UPI = PaymentMethodCreateParams.create(
        upi = PaymentMethodCreateParams.Upi(
            vpa = "8960464240@ybl"
        ),
        billingDetails = BILLING_DETAILS
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
