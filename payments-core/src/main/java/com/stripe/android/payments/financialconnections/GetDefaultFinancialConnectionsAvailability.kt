package com.stripe.android.payments.financialconnections

import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.FeatureFlags.financialConnectionsFullSdkUnavailable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GetDefaultFinancialConnectionsAvailability {
    operator fun invoke(
        isFullSdkAvailable: IsFinancialConnectionsSdkAvailable = DefaultIsFinancialConnectionsAvailable,
    ): FinancialConnectionsAvailability {
        return if (isFullSdkAvailable() && !financialConnectionsFullSdkUnavailable.isEnabled) {
            FinancialConnectionsAvailability.Full
        } else {
            FinancialConnectionsAvailability.Lite
        }
    }
}
