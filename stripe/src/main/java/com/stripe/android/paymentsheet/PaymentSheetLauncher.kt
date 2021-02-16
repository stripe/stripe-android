package com.stripe.android.paymentsheet

internal interface PaymentSheetLauncher {
    fun present(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration
    )

    fun present(
        paymentIntentClientSecret: String
    )
}
