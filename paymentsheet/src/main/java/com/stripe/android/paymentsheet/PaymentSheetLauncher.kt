package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal interface PaymentSheetLauncher {
    val id: PaymentSheet.Identifiable

    fun present(
        mode: PaymentElementLoader.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    )
}
