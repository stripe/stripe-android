package com.stripe.android.financialconnections

import android.os.Parcelable
import com.stripe.android.financialconnections.model.LinkAccountSession
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to complete a connections session
 */
sealed class FinancialConnectionsSheetResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param linkAccountSession The link account session connected
     */
    @Parcelize
    data class Completed(
        val linkAccountSession: LinkAccountSession
    ) : FinancialConnectionsSheetResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    object Canceled : FinancialConnectionsSheetResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetResult()
}
