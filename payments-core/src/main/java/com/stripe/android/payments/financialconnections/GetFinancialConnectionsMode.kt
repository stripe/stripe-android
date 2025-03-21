package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.financialconnections.FinancialConnectionsMode
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionFlags.FINANCIAL_CONNECTIONS_LITE_KILLSWITCH

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GetFinancialConnectionsMode {

    operator fun invoke(
        elementsSession: ElementsSession?
    ): FinancialConnectionsMode {
        return when {
            FeatureFlags.forceFinancialConnectionsLiteSdk.isEnabled -> {
                FinancialConnectionsMode.Lite
            }
            false -> {
                FinancialConnectionsMode.Full
            }
            elementsSession?.flags[FINANCIAL_CONNECTIONS_LITE_KILLSWITCH.flagValue] == true -> {
                FinancialConnectionsMode.None
            }
            else -> {
                FinancialConnectionsMode.Lite
            }
        }
    }
}
