package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal interface CardAccountRangeStore {
    suspend fun get(bin: String): List<CardMetadata.AccountRange>
    fun save(bin: String, accountRanges: List<CardMetadata.AccountRange>)
}
