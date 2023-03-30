package com.stripe.android.paymentsheet

internal interface PaymentSheetLauncher {

    fun present(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheetConfiguration? = null
    )
}
