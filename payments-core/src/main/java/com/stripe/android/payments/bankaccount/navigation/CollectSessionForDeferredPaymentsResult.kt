package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to create and collect a Financial Connections Session
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CollectSessionForDeferredPaymentsResult : Parcelable {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        val financialConnectionsSession: FinancialConnectionsSession
    ) : CollectSessionForDeferredPaymentsResult()

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable
    ) : CollectSessionForDeferredPaymentsResult()

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Cancelled : CollectSessionForDeferredPaymentsResult()
}
