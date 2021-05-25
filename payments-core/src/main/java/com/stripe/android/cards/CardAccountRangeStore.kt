package com.stripe.android.cards

import com.stripe.android.model.AccountRange

internal interface CardAccountRangeStore {
    suspend fun get(bin: Bin): List<AccountRange>
    fun save(bin: Bin, accountRanges: List<AccountRange>)
    suspend fun contains(bin: Bin): Boolean
}
