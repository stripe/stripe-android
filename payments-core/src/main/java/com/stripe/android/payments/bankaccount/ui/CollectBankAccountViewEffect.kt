package com.stripe.android.payments.bankaccount.ui

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent

internal sealed class CollectBankAccountViewEffect {
    data class OpenConnectionsFlow(
        val linkedAccountSessionClientSecret: String
    ) : CollectBankAccountViewEffect()

    data class FinishWithPaymentIntent(
        val paymentIntent: PaymentIntent
    ) : CollectBankAccountViewEffect()

    data class FinishWithSetupIntent(
        val setupIntent: SetupIntent
    ) : CollectBankAccountViewEffect()

    data class FinishWithError(
        val exception: Throwable
    ) : CollectBankAccountViewEffect()
}
