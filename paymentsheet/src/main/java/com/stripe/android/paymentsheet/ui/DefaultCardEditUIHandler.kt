package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DefaultCardEditUIHandler(
    override val card: PaymentMethod.Card,
    override val cardBrandFilter: CardBrandFilter,
    override val paymentMethodIcon: Int,
    override val showCardBrandDropdown: Boolean,
    private val scope: CoroutineScope,
    override val onBrandChoiceChanged: BrandChoiceCallback,
    override val onCardDetailsChanged: CardDetailsCallback
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

    init {
        scope.launch {
            cardDetailsEntry.collectLatest { state ->
                val newParams = state.takeIf {
                    it.hasChanged(
                        originalCardBrandChoice = defaultCardBrandChoice(),
                    )
                }?.toUpdateParams()
                onCardDetailsChanged(newParams)
            }
        }
    }

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
            selectedCardBrand = cardBrandChoice
        )
    }

    class Factory(
        private val scope: CoroutineScope,
        private val onBrandChoiceChanged: BrandChoiceCallback,
    ) : CardEditUIHandler.Factory {
        override fun create(
            card: PaymentMethod.Card,
            cardBrandFilter: CardBrandFilter,
            showCardBrandDropdown: Boolean,
            paymentMethodIcon: Int,
            onCardDetailsChanged: CardDetailsCallback
        ): CardEditUIHandler {
            return DefaultCardEditUIHandler(
                card = card,
                cardBrandFilter = cardBrandFilter,
                paymentMethodIcon = paymentMethodIcon,
                showCardBrandDropdown = showCardBrandDropdown,
                scope = scope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardDetailsChanged = onCardDetailsChanged
            )
        }
    }
}
