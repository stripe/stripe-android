package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal interface StaticCardAccountRanges {
    fun match(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange?
}
