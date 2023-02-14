package com.stripe.android.utils

import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.paymentsheet.PaymentSheetOrigin
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.state.PaymentSheetData
import com.stripe.android.ui.core.PaymentSheetMode

internal object PaymentSheetDataFactory {

    fun create(
        paymentMethodTypes: List<String> = listOf("card"),
        isLiveMode: Boolean = false,
        unactivatedPaymentMethodTypes: List<String> = emptyList(),
    ): PaymentSheetData = PaymentSheetData(
        id = "pi_12345",
        isLiveMode = false,
        mode = PaymentSheetMode.Payment(
            amount = 1000L,
            currency = "usd",
        ),
        setupFutureUsage = null,
        supportedPaymentMethodTypes = paymentMethodTypes,
        unactivatedPaymentMethodTypes = emptyList(),
        origin = PaymentSheetOrigin.Intent(clientSecret = PaymentIntentClientSecret("secret")),
        linkFundingSources = LinkPaymentLauncher.supportedFundingSources.toList(),
        shippingDetails = null,
    )
}
