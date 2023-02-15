package com.stripe.android.utils

import com.stripe.android.paymentsheet.state.PaymentSheetData
import com.stripe.android.ui.core.PaymentSheetMode

internal object PaymentSheetDataFactory {

    fun create(
        paymentMethodTypes: List<String>,
        isLiveMode: Boolean = false,
        unactivatedPaymentMethods: List<String> = emptyList(),
    ): PaymentSheetData {
        return PaymentSheetData(
            mode = PaymentSheetMode.Payment,
            setupFutureUse = null,
            paymentMethodTypes = paymentMethodTypes,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            isLiveMode = isLiveMode,
            linkFundingSources = emptyList(),
            shippingDetails = null,
            isLpmLevelSetupFutureUsageSet = { false },
        )
    }
}
