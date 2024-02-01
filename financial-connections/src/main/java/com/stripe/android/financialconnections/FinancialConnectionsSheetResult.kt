package com.stripe.android.financialconnections

import android.os.Parcelable
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
        val financialConnectionsSession: FinancialConnectionsSession
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
