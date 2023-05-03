package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to collect a bank account
 */
sealed class CollectBankAccountResult : Parcelable {

    @Parcelize
    data class Completed(
        val response: CollectBankAccountResponse
    ) : CollectBankAccountResult()

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : CollectBankAccountResult()

    @Parcelize
    object Cancelled : CollectBankAccountResult()
}

@Parcelize
data class CollectBankAccountResponse(
    val intent: StripeIntent,
    val financialConnectionsSession: FinancialConnectionsSession
) : StripeModel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class CollectBankAccountResultInternal : Parcelable {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Completed(
        val response: CollectBankAccountResponseInternal
    ) : CollectBankAccountResultInternal()

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failed(
        val error: Throwable
    ) : CollectBankAccountResultInternal()

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Cancelled : CollectBankAccountResultInternal()
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CollectBankAccountResponseInternal(
    val intent: StripeIntent?,
    val financialConnectionsSession: FinancialConnectionsSession
) : StripeModel
