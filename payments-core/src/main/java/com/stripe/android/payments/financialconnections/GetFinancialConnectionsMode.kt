package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsLiteKillswitch
import com.stripe.android.financialconnections.FinancialConnectionsMode
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionFlags.ELEMENTS_DISABLE_FC_LITE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GetFinancialConnectionsMode {

    operator fun invoke(
        elementsSession: ElementsSession?,
        isFinancialConnectionsFullSdkAvailable: IsFinancialConnectionsFullSdkAvailable = DefaultIsFinancialConnectionsAvailable,
    ): FinancialConnectionsMode {
        return when {
            isFinancialConnectionsFullSdkAvailable() && financialConnectionsFullSdkUnavailable.isEnabled.not() -> {
                FinancialConnectionsMode.Full
            }
            elementsSession.fcLiteKillSwitchEnabled().not() && financialConnectionsLiteKillswitch.isEnabled.not() -> {
                FinancialConnectionsMode.Lite
            }
            else -> {
                FinancialConnectionsMode.None
            }
        }
    }

    private fun ElementsSession?.fcLiteKillSwitchEnabled(): Boolean =
        this?.flags[ELEMENTS_DISABLE_FC_LITE.flagValue] == true
}
