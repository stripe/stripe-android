package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import com.stripe.android.core.model.StripeModel
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
    val clientSecret: String
) : StripeModel
