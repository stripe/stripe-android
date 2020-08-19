package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal interface CardAccountRangeStore {
    suspend fun get(bin: Bin): List<CardMetadata.AccountRange>
    fun save(bin: Bin, accountRanges: List<CardMetadata.AccountRange>)
}
