package com.stripe.android.financialconnections.launcher

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to complete a connections session
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class FinancialConnectionsSheetLinkResult : Parcelable {
    /**
     * The customer completed the connections session.
     * @param linkedAccountId The linked account id result of the AuthFlow.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        val linkedAccountId: String
    ) : FinancialConnectionsSheetLinkResult()

    /**
     * The customer canceled the connections session attempt.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Canceled : FinancialConnectionsSheetLinkResult()

    /**
     * The connections session attempt failed.
     * @param error The error encountered by the customer.
     */
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable
    ) : FinancialConnectionsSheetLinkResult()
}
