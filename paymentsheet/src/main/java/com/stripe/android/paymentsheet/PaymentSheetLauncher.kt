package com.stripe.android.paymentsheet

internal interface PaymentSheetLauncher {

    fun present(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration? = null
    )
}
