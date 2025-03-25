package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsLiteKillswitch
import com.stripe.android.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionFlags.ELEMENTS_DISABLE_FC_LITE

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GetFinancialConnectionsAvailability {

    operator fun invoke(
        elementsSession: ElementsSession?,
        isFinancialConnectionsFullSdkAvailable: IsFinancialConnectionsFullSdkAvailable = DefaultIsFinancialConnectionsAvailable,
    ): FinancialConnectionsAvailability? {
        return when {
            isFinancialConnectionsFullSdkAvailable() && financialConnectionsFullSdkUnavailable.isEnabled.not() -> {
                FinancialConnectionsAvailability.Full
            }
            elementsSession.fcLiteKillSwitchEnabled().not() && financialConnectionsLiteKillswitch.isEnabled.not() -> {
                FinancialConnectionsAvailability.Lite
            }
            else -> {
                null
            }
        }
    }

    private fun ElementsSession?.fcLiteKillSwitchEnabled(): Boolean =
        this?.flags[ELEMENTS_DISABLE_FC_LITE.flagValue] == true
}
