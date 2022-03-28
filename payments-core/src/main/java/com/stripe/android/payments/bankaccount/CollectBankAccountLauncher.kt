package com.stripe.android.payments.bankaccount

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// TODO coming in a different PR.
interface CollectBankAccountLauncher

sealed class CollectBankAccountConfiguration : Parcelable {
    @Parcelize
    data class USBankAccount(
        val name: String,
        val email: String?
    ) : Parcelable, CollectBankAccountConfiguration()
}
