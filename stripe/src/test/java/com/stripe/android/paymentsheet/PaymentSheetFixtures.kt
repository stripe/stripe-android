package com.stripe.android.paymentsheet

internal object PaymentSheetFixtures {
    internal const val CLIENT_SECRET = "client_secret"

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetActivityStarter.Args(
        CLIENT_SECRET,
        PaymentSheet.Configuration(
            customer = PaymentSheet.CustomerConfiguration(
                "customer_id",
                "ephemeral_key"
            ),
            googlePay = ConfigFixtures.GOOGLE_PAY
        )
    )

    internal val ARGS_CUSTOMER_WITHOUT_GOOGLEPAY = PaymentSheetActivityStarter.Args(
        CLIENT_SECRET,
        PaymentSheet.Configuration(
            customer = PaymentSheet.CustomerConfiguration(
                "customer_id",
                "ephemeral_key"
            )
        )
    )

    internal val ARGS_WITHOUT_CUSTOMER = PaymentSheetActivityStarter.Args(
        CLIENT_SECRET,
        config = null
    )
}
