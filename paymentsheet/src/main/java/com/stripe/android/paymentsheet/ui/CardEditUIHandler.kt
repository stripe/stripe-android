package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import kotlinx.coroutines.flow.StateFlow

internal interface CardEditUIHandler {
    val card: PaymentMethod.Card
    val cardBrandFilter: CardBrandFilter
    val state: StateFlow<State>
    val onBrandChoiceSelected: (CardBrand) -> Unit
    val onCardValuesChanged: (CardUpdateParams?) -> Unit

    fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice)

    data class State(
        val card: PaymentMethod.Card,
        val selectedCardBrand: CardBrandChoice
    )

    fun interface Factory {
        fun create(
            card: PaymentMethod.Card,
            cardBrandFilter: CardBrandFilter,
            onCardValuesChanged: (CardUpdateParams?) -> Unit
        ): CardEditUIHandler
    }
}