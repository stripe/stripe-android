package com.stripe.android.cards

import com.stripe.android.model.AccountRange

internal interface StaticCardAccountRanges {
    fun match(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange?
}
