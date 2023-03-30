package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.internal.PaymentSheetConfiguration

internal interface PaymentSheetLauncher {

    fun present(
        mode: PaymentSheet.InitializationMode,
        configuration: PaymentSheetConfiguration? = null
    )
}
