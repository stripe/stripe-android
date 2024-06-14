package com.stripe.android.cards

import com.stripe.android.model.AccountRange

internal class InMemoryCardAccountRangeStore : CardAccountRangeStore {
    private val store = mutableMapOf<Bin, List<AccountRange>>()

    override suspend fun get(bin: Bin): List<AccountRange> {
        return store.getOrElse(bin) {
            emptyList()
        }
    }

    override fun save(bin: Bin, accountRanges: List<AccountRange>) {
        store[bin] = accountRanges
    }

    override suspend fun contains(bin: Bin): Boolean {
        return store.contains(bin)
    }
}
