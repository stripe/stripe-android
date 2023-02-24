package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.airbnb.mvrx.Mavericks
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForLinkContract :
    ActivityResultContract<FinancialConnectionsSheetActivityArgs.ForLink, FinancialConnectionsSheetLinkResult>() {

    override fun createIntent(
        context: Context,
        input: FinancialConnectionsSheetActivityArgs.ForLink
    ): Intent {
        return Intent(context, FinancialConnectionsSheetActivity::class.java)
            .putExtra(Mavericks.KEY_ARG, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): FinancialConnectionsSheetLinkResult {
        return intent
            ?.getParcelableExtra<FinancialConnectionsSheetActivityResult>(EXTRA_RESULT)
            ?.toExposedResult()
            ?: FinancialConnectionsSheetLinkResult.Failed(
                IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
            )
    }

    private fun FinancialConnectionsSheetActivityResult.toExposedResult(): FinancialConnectionsSheetLinkResult =
        when (this) {
            is FinancialConnectionsSheetActivityResult.Canceled -> FinancialConnectionsSheetLinkResult.Canceled
            is FinancialConnectionsSheetActivityResult.Failed -> FinancialConnectionsSheetLinkResult.Failed(
                error
            )

            is FinancialConnectionsSheetActivityResult.Completed -> when (linkedAccountId) {
                null -> FinancialConnectionsSheetLinkResult.Failed(
                    IllegalArgumentException("linkedAccountId not set for this session")
                )

                else -> FinancialConnectionsSheetLinkResult.Completed(linkedAccountId)
            }
        }
}
