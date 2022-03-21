package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import com.stripe.android.payments.bankaccount.CollectBankAccountResponse
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
