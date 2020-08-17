package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal interface CardAccountRangeSource {
    suspend fun getAccountRange(cardNumber: String): CardMetadata.AccountRange?
}
