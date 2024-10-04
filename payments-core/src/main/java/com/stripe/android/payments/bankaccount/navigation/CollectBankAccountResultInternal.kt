package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

/**
 * Result used internally to communicate between
 * [com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity] and
 * instances of [com.stripe.android.payments.bankaccount.CollectBankAccountLauncher].
 *
 * This class is not intended to be used by external classes.
 */
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
    data object Cancelled : CollectBankAccountResultInternal()
}

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CollectBankAccountResponseInternal(
    val intent: StripeIntent?,
    val usBankAccountData: USBankAccountData?,
    val instantDebitsData: InstantDebitsData?,
) : StripeModel {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class USBankAccountData(
        val financialConnectionsSession: FinancialConnectionsSession
    ) : StripeModel

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class InstantDebitsData(
        val paymentMethod: PaymentMethod,
        val last4: String?,
        val bankName: String?,
        val incentiveEligible: Boolean,
    ) : StripeModel
}
