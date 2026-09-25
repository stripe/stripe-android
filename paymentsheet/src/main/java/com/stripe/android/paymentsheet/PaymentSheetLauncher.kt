package com.stripe.android.paymentsheet

import com.stripe.android.core.Identifiable
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal interface PaymentSheetLauncher {

    fun present(
        id: Identifiable,
        mode: PaymentElementLoader.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    )
}
