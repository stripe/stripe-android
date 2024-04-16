package com.stripe.android.financialconnections

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to complete a connections session
 */
sealed class FinancialConnectionsSheetForLinkResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param financialConnectionsSession The financial connections session connected
     */
    @Parcelize
    data class Completed(
        val linkAccountId: String,
    ) : FinancialConnectionsSheetForLinkResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    data object Canceled : FinancialConnectionsSheetForLinkResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetForLinkResult()
}
