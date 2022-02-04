package com.stripe.android.cards

import com.stripe.android.model.AccountRange

internal interface CardAccountRangeStore {
    suspend fun get(bin: com.stripe.android.cards.Bin): List<AccountRange>
    fun save(bin: com.stripe.android.cards.Bin, accountRanges: List<AccountRange>)
    suspend fun contains(bin: com.stripe.android.cards.Bin): Boolean
}
