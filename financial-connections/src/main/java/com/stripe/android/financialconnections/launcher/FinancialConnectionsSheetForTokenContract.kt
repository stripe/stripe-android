package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.airbnb.mvrx.Mavericks
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT

internal class FinancialConnectionsSheetForTokenContract :
    ActivityResultContract<FinancialConnectionsSheetActivityArgs.ForToken, FinancialConnectionsSheetForTokenResult>() {

    override fun createIntent(
        context: Context,
        input: FinancialConnectionsSheetActivityArgs.ForToken
    ): Intent {
        return Intent(context, FinancialConnectionsSheetActivity::class.java)
            .putExtra(Mavericks.KEY_ARG, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): FinancialConnectionsSheetForTokenResult {
        return intent
            ?.getParcelableExtra<FinancialConnectionsSheetActivityResult>(EXTRA_RESULT)
            ?.toExposedResult()
            ?: FinancialConnectionsSheetForTokenResult.Failed(
                IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
            )
    }

    private fun FinancialConnectionsSheetActivityResult.toExposedResult(): FinancialConnectionsSheetForTokenResult =
        when (this) {
            is FinancialConnectionsSheetActivityResult.Canceled -> FinancialConnectionsSheetForTokenResult.Canceled
            is FinancialConnectionsSheetActivityResult.Failed -> FinancialConnectionsSheetForTokenResult.Failed(
                error
            )
            is FinancialConnectionsSheetActivityResult.Completed -> FinancialConnectionsSheetForTokenResult.Completed(
                financialConnectionsSession = financialConnectionsSession,
                token = requireNotNull(token)
            )
        }
}
