package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsLiteEnabled
import com.stripe.android.model.ElementsSession

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GetFinancialConnectionsAvailability {

    operator fun invoke(
        elementsSession: ElementsSession?,
        isFullSdkAvailable: IsFinancialConnectionsSdkAvailable = DefaultIsFinancialConnectionsAvailable,
    ): FinancialConnectionsAvailability? {
        return when {
            isFullSdkAvailable() && financialConnectionsFullSdkUnavailable.isEnabled.not() -> {
                FinancialConnectionsAvailability.Full
            }
            elementsSession.fcLiteKillSwitchEnabled().not() && financialConnectionsLiteEnabled.isEnabled -> {
                FinancialConnectionsAvailability.Lite
            }
            else -> {
                null
            }
        }
    }

    private fun ElementsSession?.fcLiteKillSwitchEnabled(): Boolean =
        this?.flags[ElementsSession.Flag.ELEMENTS_DISABLE_FC_LITE] == true
}
