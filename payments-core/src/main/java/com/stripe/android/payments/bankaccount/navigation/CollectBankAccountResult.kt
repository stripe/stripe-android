package com.stripe.android.payments.bankaccount.navigation

import android.os.Parcelable
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to collect a bank account
 */
internal sealed class CollectBankAccountResult : Parcelable {

    // TODO manage setup and payment intents.
    @Parcelize
    data class Completed(
        val intent: StripeIntent
    ) : CollectBankAccountResult()

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : CollectBankAccountResult()
}
