package com.stripe.android.paymentsheet.ui

import app.cash.turbine.Turbine
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FakeCardEditUIHandler(
    override val card: PaymentMethod.Card,
    override val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    override val paymentMethodIcon: Int = 0,
    override val showCardBrandDropdown: Boolean = true,
    override val onBrandChoiceChanged: BrandChoiceCallback = {},
    override val onCardValuesChanged: CardValuesCallback = {}
) : CardEditUIHandler {
    override val state: MutableStateFlow<CardEditUIHandler.State> = MutableStateFlow(
        value = CardEditUIHandler.State(
            card = card,
            selectedCardBrand = card.getPreferredChoice(cardBrandFilter)
        )
    )
    private val onBrandChoiceChangedCalls = Turbine<CardBrandChoice>()

    override fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        onBrandChoiceChangedCalls.add(cardBrandChoice)
    }

    fun cardBrandChanged(cardBrandChoice: CardBrandChoice) {
        state.update {
            it.copy(selectedCardBrand = cardBrandChoice)
        }
    }

    suspend fun awaitBrandChoiceChangedCall() = onBrandChoiceChangedCalls.awaitItem()

    fun ensureAllEventsConsumed() {
        onBrandChoiceChangedCalls.ensureAllEventsConsumed()
    }
}
