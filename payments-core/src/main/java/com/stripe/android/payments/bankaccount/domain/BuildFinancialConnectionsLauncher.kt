package com.stripe.android.payments.bankaccount.domain

import android.content.Intent
import androidx.activity.ComponentActivity
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.intentBuilder
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForInstantDebitsLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetInstantDebitsResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher
import com.stripe.android.financialconnections.lite.liteIntentBuilder
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability

internal object BuildFinancialConnectionsLauncher {
    operator fun invoke(
        activity: ComponentActivity,
        configuration: CollectBankAccountConfiguration,
        financialConnectionsAvailability: FinancialConnectionsAvailability,
        onConnectionsForInstantDebitsResult: (FinancialConnectionsSheetInstantDebitsResult) -> Unit,
        onConnectionsForACHResult: (FinancialConnectionsSheetResult) -> Unit
    ): FinancialConnectionsSheetLauncher {
        return when (configuration) {
            is CollectBankAccountConfiguration.InstantDebits -> FinancialConnectionsSheetForInstantDebitsLauncher(
                activity = activity,
                callback = onConnectionsForInstantDebitsResult,
                intentBuilder = financialConnectionsAvailability.getIntentBuilder(activity)
            )

            is CollectBankAccountConfiguration.USBankAccount,
            is CollectBankAccountConfiguration.USBankAccountInternal -> FinancialConnectionsSheetForDataLauncher(
                activity = activity,
                callback = onConnectionsForACHResult,
                intentBuilder = financialConnectionsAvailability.getIntentBuilder(activity)
            )
        }
    }

    private fun FinancialConnectionsAvailability.getIntentBuilder(
        activity: ComponentActivity
    ): (FinancialConnectionsSheetActivityArgs) -> Intent {
        return when (this) {
            FinancialConnectionsAvailability.Full -> intentBuilder(activity)
            FinancialConnectionsAvailability.Lite -> liteIntentBuilder(activity)
        }
    }
}
