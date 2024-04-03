package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheetActivity
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForInstantDebitsContract :
    ActivityResultContract<FinancialConnectionsSheetActivityArgs.ForInstantDebits, FinancialConnectionsSheetInstantDebitsResult>() {

    override fun createIntent(
        context: Context,
        input: FinancialConnectionsSheetActivityArgs.ForInstantDebits
    ): Intent = FinancialConnectionsSheetActivity.intent(
        context = context,
        args = input
    )

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): FinancialConnectionsSheetInstantDebitsResult {
        return intent
            ?.getParcelableExtra<FinancialConnectionsSheetActivityResult>(EXTRA_RESULT)
            ?.toExposedResult()
            ?: FinancialConnectionsSheetInstantDebitsResult.Failed(
                IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
            )
    }

    private fun FinancialConnectionsSheetActivityResult.toExposedResult(): FinancialConnectionsSheetInstantDebitsResult =
        when (this) {
            is FinancialConnectionsSheetActivityResult.Canceled -> FinancialConnectionsSheetInstantDebitsResult.Canceled
            is FinancialConnectionsSheetActivityResult.Failed -> FinancialConnectionsSheetInstantDebitsResult.Failed(
                error
            )

            is FinancialConnectionsSheetActivityResult.Completed -> when (paymentMethodId) {
                null -> FinancialConnectionsSheetInstantDebitsResult.Failed(
                    IllegalArgumentException("linkedAccountId not set for this session")
                )

                else -> FinancialConnectionsSheetInstantDebitsResult.Completed(paymentMethodId)
            }
        }
}
