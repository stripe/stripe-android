package com.stripe.android.paymentsheet

internal object ConfigFixtures {
    val GOOGLE_PAY = PaymentSheet.GooglePayConfiguration(
        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
        countryCode = "US",
        currencyCode = "USD"
    )
}
