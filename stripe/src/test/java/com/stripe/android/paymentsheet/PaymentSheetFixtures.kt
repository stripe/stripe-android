package com.stripe.android.paymentsheet

internal object PaymentSheetFixtures {
    private const val MERCHANT_DISPLAY_NAME = "Widget Store"
    internal const val CLIENT_SECRET = "client_secret"

    internal val CONFIG_MINIMUM = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME
    )

    internal val CONFIG_CUSTOMER = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = PaymentSheet.CustomerConfiguration(
            "customer_id",
            "ephemeral_key"
        )
    )

    internal val CONFIG_GOOGLEPAY = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        googlePay = ConfigFixtures.GOOGLE_PAY
    )

    internal val CONFIG_CUSTOMER_WITH_GOOGLEPAY = CONFIG_CUSTOMER.copy(
        googlePay = ConfigFixtures.GOOGLE_PAY
    )

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetActivityStarter.Args(
        CLIENT_SECRET,
        CONFIG_CUSTOMER_WITH_GOOGLEPAY
    )

    internal val ARGS_CUSTOMER_WITHOUT_GOOGLEPAY = PaymentSheetActivityStarter.Args(
        CLIENT_SECRET,
        CONFIG_CUSTOMER
    )

    internal val ARGS_WITHOUT_CUSTOMER = PaymentSheetActivityStarter.Args(
        CLIENT_SECRET,
        config = null
    )
}
