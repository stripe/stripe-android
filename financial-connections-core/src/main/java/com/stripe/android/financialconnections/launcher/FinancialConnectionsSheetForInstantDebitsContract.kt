package com.stripe.android.financialconnections.launcher

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForInstantDebits
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Companion.EXTRA_RESULT
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetInstantDebitsResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetInstantDebitsResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetInstantDebitsResult.Failed

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FinancialConnectionsSheetForInstantDebitsContract(
    private val intentBuilder: (FinancialConnectionsSheetActivityArgs) -> Intent
) :
    ActivityResultContract<ForInstantDebits, FinancialConnectionsSheetInstantDebitsResult>() {

    override fun createIntent(
        context: Context,
        input: ForInstantDebits
    ): Intent = intentBuilder(input)

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): FinancialConnectionsSheetInstantDebitsResult {
        return intent
            ?.getParcelableExtra<FinancialConnectionsSheetActivityResult>(EXTRA_RESULT)
            ?.toExposedResult()
            ?: Failed(
                IllegalArgumentException("Failed to retrieve a ConnectionsSheetResult.")
            )
    }

    private fun FinancialConnectionsSheetActivityResult.toExposedResult():
        FinancialConnectionsSheetInstantDebitsResult = when (this) {
        is FinancialConnectionsSheetActivityResult.Canceled -> Canceled
        is FinancialConnectionsSheetActivityResult.Failed -> Failed(error)
        is FinancialConnectionsSheetActivityResult.Completed -> when (instantDebits) {
            null -> Failed(IllegalArgumentException("Instant debits result is missing"))
            else -> Completed(
                encodedPaymentMethod = instantDebits.encodedPaymentMethod,
                last4 = instantDebits.last4,
                bankName = instantDebits.bankName,
                eligibleForIncentive = instantDebits.eligibleForIncentive,
            )
        }
    }
}
