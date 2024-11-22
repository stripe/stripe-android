package com.stripe.android.financialconnections

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.FinancialConnectionsSheetInternalResult.Canceled
import com.stripe.android.financialconnections.FinancialConnectionsSheetInternalResult.Completed
import com.stripe.android.financialconnections.FinancialConnectionsSheetInternalResult.Failed
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to complete a connections session
 */
sealed class FinancialConnectionsSheetResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param financialConnectionsSession The financial connections session connected
     */
    @Parcelize
    data class Completed(
        val financialConnectionsSession: FinancialConnectionsSession,
    ) : FinancialConnectionsSheetResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    data object Canceled : FinancialConnectionsSheetResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetResult()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FinancialConnectionsSheetInternalResult : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Completed(
        val financialConnectionsSession: FinancialConnectionsSession,
        val manualEntryUsesMicrodeposits: Boolean,
    ) : FinancialConnectionsSheetInternalResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data object Canceled : FinancialConnectionsSheetInternalResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetInternalResult()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun FinancialConnectionsSheetInternalResult.toPublicResult(): FinancialConnectionsSheetResult {
    return when (this) {
        is Canceled -> FinancialConnectionsSheetResult.Canceled
        is Failed -> FinancialConnectionsSheetResult.Failed(error)
        is Completed -> FinancialConnectionsSheetResult.Completed(financialConnectionsSession)
    }
}
