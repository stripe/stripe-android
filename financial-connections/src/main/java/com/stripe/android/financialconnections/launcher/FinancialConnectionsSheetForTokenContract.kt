package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.airbnb.mvrx.Mavericks
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult.Canceled
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult.Completed
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult.Failed
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
            ?: Failed(
                IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
            )
    }

    private fun FinancialConnectionsSheetActivityResult.toExposedResult(): FinancialConnectionsSheetForTokenResult =
        when (this) {
            is FinancialConnectionsSheetActivityResult.Canceled -> Canceled
            is FinancialConnectionsSheetActivityResult.Failed -> Failed(
                error
            )

            is FinancialConnectionsSheetActivityResult.Completed -> when {
                financialConnectionsSession == null -> Failed(
                    IllegalArgumentException("FinancialConnectionsSession is not set")
                )

                token == null -> Failed(
                    IllegalArgumentException("PaymentAccount is not set on FinancialConnectionsSession")
                )

                else -> Completed(
                    financialConnectionsSession = financialConnectionsSession,
                    token = token
                )
            }
        }
}
