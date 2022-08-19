package com.stripe.android.financialconnections.link

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to complete a connections session
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FinancialConnectionsSheetLinkResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param financialConnectionsSession The financial connections session connected
     */
    @Parcelize
    data class Completed(
        val linkedAccountId: String
    ) : FinancialConnectionsSheetLinkResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    object Canceled : FinancialConnectionsSheetLinkResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetLinkResult()
}
