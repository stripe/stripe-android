package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class DefaultCardEditUIHandler(
    override val card: PaymentMethod.Card,
    override val cardBrandFilter: CardBrandFilter,
    override val paymentMethodIcon: Int,
    override val showCardBrandDropdown: Boolean,
    scope: CoroutineScope,
    override val onBrandChoiceChanged: (CardBrand) -> Unit,
    override val onCardValuesChanged: (CardUpdateParams?) -> Unit
) : CardEditUIHandler {
    private val cardDetailsEntry = MutableStateFlow(
        value = buildDefaultCardEntry()
    )

    override val state: StateFlow<CardEditUIHandler.State> = cardDetailsEntry.mapLatest { inputState ->
        uiState(inputState.cardBrandChoice)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = uiState()
    )

    override fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        if (cardBrandChoice != state.value.selectedCardBrand) {
            onBrandChoiceChanged(cardBrandChoice.brand)
        }
        cardDetailsEntry.update {
            it.copy(
                cardBrandChoice = cardBrandChoice
            )
        }
    }

    private fun buildDefaultCardEntry(): CardDetailsEntry {
        return CardDetailsEntry(
            cardBrandChoice = defaultCardBrandChoice()
        )
    }

    private fun defaultCardBrandChoice() = card.getPreferredChoice(cardBrandFilter)

    private fun uiState(cardBrandChoice: CardBrandChoice = defaultCardBrandChoice()): CardEditUIHandler.State {
        return CardEditUIHandler.State(
            card = card,
            selectedCardBrand = cardBrandChoice
        )
    }

    class Factory(
        private val scope: CoroutineScope,
        private val onBrandChoiceChanged: (CardBrand) -> Unit,
    ) : CardEditUIHandler.Factory {
        override fun create(
            card: PaymentMethod.Card,
            cardBrandFilter: CardBrandFilter,
            showCardBrandDropdown: Boolean,
            paymentMethodIcon: Int,
            onCardValuesChanged: (CardUpdateParams?) -> Unit
        ): CardEditUIHandler {
            return DefaultCardEditUIHandler(
                card = card,
                cardBrandFilter = cardBrandFilter,
                paymentMethodIcon = paymentMethodIcon,
                showCardBrandDropdown = showCardBrandDropdown,
                scope = scope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardValuesChanged = onCardValuesChanged
            )
        }

    }
}