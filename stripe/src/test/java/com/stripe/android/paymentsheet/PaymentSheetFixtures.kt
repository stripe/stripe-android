package com.stripe.android.paymentsheet

internal object PaymentSheetFixtures {
    internal const val CLIENT_SECRET = "client_secret"

    internal val DEFAULT_ARGS = PaymentSheetActivityStarter.Args.Default(
        CLIENT_SECRET,
        "ephemeral_key",
        "customer_id"
    )

    internal val GUEST_ARGS = PaymentSheetActivityStarter.Args.Guest(
        CLIENT_SECRET
    )
}
