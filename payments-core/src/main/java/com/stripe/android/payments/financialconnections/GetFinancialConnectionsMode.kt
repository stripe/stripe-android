package com.stripe.android.payments.financialconnections

import android.util.Log
import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.financialconnections.FinancialConnectionsMode
import com.stripe.android.model.ElementsSession

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GetFinancialConnectionsMode {

    operator fun invoke(
        elementsSession: ElementsSession?
    ): FinancialConnectionsMode {
        return when {
            FeatureFlags.forceFinancialConnectionsLiteSdk.isEnabled -> {
                Log.d("FCMode", "lite because of force flag")
                FinancialConnectionsMode.Lite
            }
            false -> {
                Log.d("FCMode", "full because of available")
                FinancialConnectionsMode.Full
            }
            elementsSession?.flags["financial-connections-lite-killswitch"] == true -> {
                Log.d("FCMode", "none because of killswitch")
                FinancialConnectionsMode.None
            }
            else -> {
                Log.d("FCMode", "lite")
                FinancialConnectionsMode.Lite
            }
        }
    }
}
