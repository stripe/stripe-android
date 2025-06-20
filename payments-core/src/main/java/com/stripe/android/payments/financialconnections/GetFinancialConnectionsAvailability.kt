package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable
import com.stripe.android.model.ElementsSession

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GetFinancialConnectionsAvailability {

    operator fun invoke(
        elementsSession: ElementsSession?,
        isFullSdkAvailable: IsFinancialConnectionsSdkAvailable = DefaultIsFinancialConnectionsAvailable,
    ): FinancialConnectionsAvailability? {
        return when {
            elementsSession.preferLite() && elementsSession.fcLiteKillSwitchEnabled().not() -> {
                FinancialConnectionsAvailability.Lite
            }
            isFullSdkAvailable() && financialConnectionsFullSdkUnavailable.isEnabled.not() -> {
                FinancialConnectionsAvailability.Full
            }
            elementsSession.fcLiteKillSwitchEnabled().not() -> {
                FinancialConnectionsAvailability.Lite
            }
            else -> {
                null
            }
        }
    }

    private fun ElementsSession?.fcLiteKillSwitchEnabled(): Boolean =
        this?.flags?.get(ElementsSession.Flag.ELEMENTS_DISABLE_FC_LITE) == true

    private fun ElementsSession?.preferLite(): Boolean =
        this?.flags?.get(ElementsSession.Flag.ELEMENTS_PREFER_FC_LITE) == true
}
