package com.stripe.android.paymentsheet

internal interface PaymentSheetLauncher {
    fun present(
        intentClientSecret: String,
        configuration: PaymentSheet.Configuration
    )

    fun present(
        intentClientSecret: String
    )
}
