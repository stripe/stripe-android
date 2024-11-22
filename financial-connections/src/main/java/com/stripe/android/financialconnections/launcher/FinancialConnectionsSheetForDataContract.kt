package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity
import com.stripe.android.financialconnections.FinancialConnectionsSheetInternalResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT

internal class FinancialConnectionsSheetForDataContract :
    ActivityResultContract<FinancialConnectionsSheetActivityArgs.ForData, FinancialConnectionsSheetInternalResult>() {

    override fun createIntent(
        context: Context,
        input: FinancialConnectionsSheetActivityArgs.ForData
    ): Intent {
        return FinancialConnectionsSheetActivity.intent(
            context = context,
            args = input
        )
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): FinancialConnectionsSheetInternalResult {
        return intent
            ?.getParcelableExtra<FinancialConnectionsSheetActivityResult>(EXTRA_RESULT)
            ?.toResult()
            ?: FinancialConnectionsSheetInternalResult.Failed(
                IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
            )
    }

    private fun FinancialConnectionsSheetActivityResult.toResult(): FinancialConnectionsSheetInternalResult =
        when (this) {
            is FinancialConnectionsSheetActivityResult.Canceled -> FinancialConnectionsSheetInternalResult.Canceled
            is FinancialConnectionsSheetActivityResult.Failed -> FinancialConnectionsSheetInternalResult.Failed(
                error
            )

            is FinancialConnectionsSheetActivityResult.Completed ->
                when (financialConnectionsSession) {
                    null -> FinancialConnectionsSheetInternalResult.Failed(
                        IllegalArgumentException("financialConnectionsSession not set.")
                    )

                    else -> FinancialConnectionsSheetInternalResult.Completed(
                        financialConnectionsSession = financialConnectionsSession,
                        manualEntryUsesMicrodeposits = manualEntryUsesMicrodeposits,
                    )
                }
        }
}
