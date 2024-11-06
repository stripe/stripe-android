package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal interface PaymentSheetLauncher {

    fun present(
        mode: PaymentElementLoader.InitializationMode,
        configuration: PaymentSheet.Configuration? = null
    )
}
