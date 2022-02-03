package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import kotlinx.coroutines.flow.Flow

internal interface CardAccountRangeSource {
    suspend fun getAccountRange(
        cardNumber: com.stripe.android.ui.core.elements.CardNumber.Unvalidated
    ): AccountRange?

    val loading: Flow<Boolean>
}
