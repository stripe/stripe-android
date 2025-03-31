package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult.Canceled
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult.Completed
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult.Failed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForTokenContract(
    private val intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent
) :
    ActivityResultContract<FinancialConnectionsSheetActivityArgs.ForToken, FinancialConnectionsSheetForTokenResult>() {

    override fun createIntent(
        context: Context,
        input: FinancialConnectionsSheetActivityArgs.ForToken
    ): Intent = intentBuilder(input)

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
