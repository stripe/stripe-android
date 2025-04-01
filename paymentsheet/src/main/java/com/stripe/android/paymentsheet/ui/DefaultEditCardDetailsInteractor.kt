package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class DefaultEditCardDetailsInteractor private constructor(
    private val card: PaymentMethod.Card,
    private val cardBrandFilter: CardBrandFilter,
    private val isModifiable: Boolean,
    private val scope: CoroutineScope,
    private val onBrandChoiceChanged: CardBrandCallback,
    override val onCardUpdateParamsChanged: CardUpdateParamsCallback
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
                onCardUpdateParamsChanged(newParams)
            }
        }
    }

    private fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        if (cardBrandChoice != state.value.selectedCardBrand) {
            onBrandChoiceChanged(cardBrandChoice.brand)
        }
        cardDetailsEntry.update {
            it.copy(
                cardBrandChoice = cardBrandChoice
            )
        }
    }

    override fun handleViewAction(viewAction: EditCardDetailsInteractor.ViewAction) {
        when (viewAction) {
            is EditCardDetailsInteractor.ViewAction.BrandChoiceChanged -> {
                onBrandChoiceChanged(viewAction.cardBrandChoice)
            }
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
            selectedCardBrand = cardBrandChoice,
            paymentMethodIcon = card.getSavedPaymentMethodIcon(forVerticalMode = true),
            shouldShowCardBrandDropdown = isModifiable && isExpired().not(),
            availableNetworks = card.getAvailableNetworks(cardBrandFilter)
        )
    }

    private fun isExpired(): Boolean {
        val cardExpiryMonth = card.expiryMonth
        val cardExpiryYear = card.expiryYear
        // If the card's expiration dates are missing, we can't conclude that it is expired, so we don't want to
        // show the user an expired card error.
        return cardExpiryMonth != null && cardExpiryYear != null &&
            !DateUtils.isExpiryDataValid(
                expiryMonth = cardExpiryMonth,
                expiryYear = cardExpiryYear,
            )
    }

    class Factory(
        private val workContext: CoroutineContext,
        private val isModifiable: Boolean,
        private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        private val onBrandChoiceChanged: CardBrandCallback,
    ) : EditCardDetailsInteractor.Factory {
        override fun create(
            card: PaymentMethod.Card,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor {
            return DefaultEditCardDetailsInteractor(
                card = card,
                cardBrandFilter = cardBrandFilter,
                scope = CoroutineScope(workContext + SupervisorJob()),
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardUpdateParamsChanged = onCardUpdateParamsChanged,
                isModifiable = isModifiable
            )
        }
    }
}
