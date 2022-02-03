package com.stripe.android.cards

import com.stripe.android.model.AccountRange

internal interface StaticCardAccountRanges {
    /**
     * Return the first [AccountRange] that contains the given [cardNumber], or `null`.
     */
    fun first(
        cardNumber: com.stripe.android.ui.core.elements.CardNumber.Unvalidated
    ): AccountRange?

    /**
     * Return all [AccountRange]s that contain the given [cardNumber].
     */
    fun filter(cardNumber: com.stripe.android.ui.core.elements.CardNumber.Unvalidated): List<AccountRange>
}
