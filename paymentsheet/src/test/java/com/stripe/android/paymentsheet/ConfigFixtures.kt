package com.stripe.android.paymentsheet

import com.stripe.android.elements.payment.GooglePayConfiguration

internal object ConfigFixtures {
    val GOOGLE_PAY = GooglePayConfiguration(
        environment = GooglePayConfiguration.Environment.Test,
        countryCode = "US",
        currencyCode = "USD"
    )
}
