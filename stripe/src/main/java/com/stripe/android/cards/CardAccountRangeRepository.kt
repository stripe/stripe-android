package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal interface CardAccountRangeRepository {
    suspend fun getAccountRange(cardNumber: String): CardMetadata.AccountRange?
}
