package com.stripe.android.paymentsheet.state

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.PaymentSheetMode
import com.stripe.android.ui.core.PaymentSheetSetupFutureUse

internal data class PaymentSheetData(
    val mode: PaymentSheetMode,
    val setupFutureUse: PaymentSheetSetupFutureUse?,
    val paymentMethodTypes: List<String>,
    val unactivatedPaymentMethods: List<String>,
    val isLiveMode: Boolean,
    val linkFundingSources: List<String>,
    val shippingDetails: PaymentIntent.Shipping?,
    val isLpmLevelSetupFutureUsageSet: (code: PaymentMethodCode) -> Boolean,
)

internal fun StripeIntent.toPaymentSheetData(): PaymentSheetData {
    return PaymentSheetData(
        mode = mode(),
        setupFutureUse = setupFutureUse(),
        paymentMethodTypes = paymentMethodTypes,
        unactivatedPaymentMethods = unactivatedPaymentMethods,
        isLiveMode = isLiveMode,
        linkFundingSources = linkFundingSources,
        shippingDetails = (this as? PaymentIntent)?.shipping,
        isLpmLevelSetupFutureUsageSet = { code ->
            (this as? PaymentIntent)?.isLpmLevelSetupFutureUsageSet(code) ?: false
        },
    )
}
