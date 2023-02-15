package com.stripe.android.utils

import com.stripe.android.paymentsheet.PaymentSheetOrigin
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.state.PaymentSheetData
import com.stripe.android.ui.core.PaymentSheetMode

internal object PaymentSheetDataFactory {

    fun create(
        paymentMethodTypes: List<String>,
        isLiveMode: Boolean = false,
        unactivatedPaymentMethods: List<String> = emptyList(),
    ): PaymentSheetData {
        return PaymentSheetData(
            id = null,
            mode = PaymentSheetMode.Payment(
                amount = 1_000L,
                currency = "usd",
            ),
            setupFutureUse = null,
            paymentMethodTypes = paymentMethodTypes,
            unactivatedPaymentMethods = unactivatedPaymentMethods,
            isLiveMode = isLiveMode,
            linkFundingSources = emptyList(),
            shippingDetails = null,
            origin = PaymentSheetOrigin.Intent(
                clientSecret = PaymentIntentClientSecret("secret"),
            )
        )
    }
}
