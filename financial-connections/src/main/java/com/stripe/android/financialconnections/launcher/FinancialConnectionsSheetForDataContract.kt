package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.airbnb.android.showkase.models.Showkase
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.getBrowserIntent
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT

internal class FinancialConnectionsSheetForDataContract :
    ActivityResultContract<FinancialConnectionsSheetActivityArgs.ForData, FinancialConnectionsSheetResult>() {

    override fun createIntent(
        context: Context,
        input: FinancialConnectionsSheetActivityArgs.ForData
    ): Intent {
        return Showkase.getBrowserIntent(context)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): FinancialConnectionsSheetResult {
        return intent
            ?.getParcelableExtra<FinancialConnectionsSheetActivityResult>(EXTRA_RESULT)
            ?.toExposedResult()
            ?: FinancialConnectionsSheetResult.Failed(
                IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
            )
    }

    private fun FinancialConnectionsSheetActivityResult.toExposedResult(): FinancialConnectionsSheetResult =
        when (this) {
            is FinancialConnectionsSheetActivityResult.Canceled -> FinancialConnectionsSheetResult.Canceled
            is FinancialConnectionsSheetActivityResult.Failed -> FinancialConnectionsSheetResult.Failed(error)
            is FinancialConnectionsSheetActivityResult.Completed -> FinancialConnectionsSheetResult.Completed(
                financialConnectionsSession = financialConnectionsSession
            )
        }
}
