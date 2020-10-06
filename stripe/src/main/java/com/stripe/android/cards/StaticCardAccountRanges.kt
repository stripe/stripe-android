package com.stripe.android.cards

import com.stripe.android.model.AccountRange

internal interface StaticCardAccountRanges {
    /**
     * Return the first [AccountRange] that contains the given [cardNumber], or `null`.
     */
    fun first(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange?

    /**
     * Return all [AccountRange]s that contain the given [cardNumber].
     */
    fun filter(cardNumber: CardNumber.Unvalidated): List<AccountRange>
}
