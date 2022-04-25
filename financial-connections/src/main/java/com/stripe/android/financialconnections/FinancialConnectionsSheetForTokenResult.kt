package com.stripe.android.connections

import android.os.Parcelable
import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.model.Token
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to complete a connections session
 */
sealed class FinancialConnectionsSheetForTokenResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param linkAccountSession The link account session connected
     */
    @Parcelize
    data class Completed(
        val linkAccountSession: LinkAccountSession,
        val token: Token
    ) : FinancialConnectionsSheetForTokenResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    object Canceled : FinancialConnectionsSheetForTokenResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetForTokenResult()
}
