package com.stripe.android.paymentsheet.prototype

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.SharedDataSpec

internal class ParsingMetadata(
    val stripeIntent: StripeIntent,
    val configuration: PaymentSheet.Configuration,
    val sharedDataSpecs: List<SharedDataSpec>,
    val isDeferred: Boolean,
    val financialConnectionsAvailable: IsFinancialConnectionsAvailable = DefaultIsFinancialConnectionsAvailable()
) {
    val merchantName: String = configuration.merchantDisplayName

    fun hasIntentToSetup(): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.setupFutureUsage != null
            is SetupIntent -> true
        }
    }
}
