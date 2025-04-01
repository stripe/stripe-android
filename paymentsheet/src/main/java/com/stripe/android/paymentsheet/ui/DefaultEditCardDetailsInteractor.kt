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

internal class DefaultEditCardDetailsInteractor(
    override val card: PaymentMethod.Card,
    override val cardBrandFilter: CardBrandFilter,
    override val paymentMethodIcon: Int,
    override val shouldShowCardBrandDropdown: Boolean,
    private val scope: CoroutineScope,
    override val onBrandChoiceChanged: CardBrandCallback,
    override val onCardDetailsChanged: CardUpdateParamsCallback
) : EditCardDetailsInteractor {
    private val cardDetailsEntry = MutableStateFlow(
        value = buildDefaultCardEntry()
    )

    override val state: StateFlow<EditCardDetailsInteractor.State> = cardDetailsEntry.mapLatest { inputState ->
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

    private fun uiState(cardBrandChoice: CardBrandChoice = defaultCardBrandChoice()): EditCardDetailsInteractor.State {
        return EditCardDetailsInteractor.State(
            card = card,
            selectedCardBrand = cardBrandChoice
        )
    }

    class Factory(
        private val scope: CoroutineScope,
        private val onBrandChoiceChanged: CardBrandCallback,
    ) : EditCardDetailsInteractor.Factory {
        override fun create(
            card: PaymentMethod.Card,
            cardBrandFilter: CardBrandFilter,
            showCardBrandDropdown: Boolean,
            paymentMethodIcon: Int,
            onCardDetailsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor {
            return DefaultEditCardDetailsInteractor(
                card = card,
                cardBrandFilter = cardBrandFilter,
                paymentMethodIcon = paymentMethodIcon,
                shouldShowCardBrandDropdown = showCardBrandDropdown,
                scope = scope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardDetailsChanged = onCardDetailsChanged
            )
        }
    }
}
