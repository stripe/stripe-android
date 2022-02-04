package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import com.stripe.android.ui.core.elements.CardNumber
import kotlinx.coroutines.flow.Flow

internal interface CardAccountRangeSource {
    suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange?

    val loading: Flow<Boolean>
}
