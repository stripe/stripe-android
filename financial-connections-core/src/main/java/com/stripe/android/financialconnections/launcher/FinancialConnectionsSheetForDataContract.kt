package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForDataContract(
    val intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent
) : ActivityResultContract<FinancialConnectionsSheetActivityArgs.ForData, FinancialConnectionsSheetResult>() {

    override fun createIntent(
        context: Context,
        input: FinancialConnectionsSheetActivityArgs.ForData
    ): Intent {
        return intentBuilder(input)
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
            is FinancialConnectionsSheetActivityResult.Failed -> FinancialConnectionsSheetResult.Failed(
                error
            )

            is FinancialConnectionsSheetActivityResult.Completed ->
                when (financialConnectionsSession) {
                    null -> FinancialConnectionsSheetResult.Failed(
                        IllegalArgumentException("financialConnectionsSession not set.")
                    )

                    else -> FinancialConnectionsSheetResult.Completed(
                        financialConnectionsSession = financialConnectionsSession
                    )
                }
        }
}
