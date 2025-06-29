package com.stripe.android.payments.bankaccount.domain

import androidx.activity.ComponentActivity
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForDataLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetForInstantDebitsLauncher
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetInstantDebitsResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.payments.financialconnections.getIntentBuilder

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
}
