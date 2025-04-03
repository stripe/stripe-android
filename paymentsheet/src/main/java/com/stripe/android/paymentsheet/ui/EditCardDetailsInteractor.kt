package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal typealias CardUpdateParamsCallback = (CardUpdateParams?) -> Unit

internal typealias CardBrandCallback = (CardBrand) -> Unit

internal interface EditCardDetailsInteractor {
    val state: StateFlow<State>

    val onCardUpdateParamsChanged: CardUpdateParamsCallback

    fun handleViewAction(viewAction: ViewAction)

    @Immutable
    data class State(
        val card: PaymentMethod.Card,
        val selectedCardBrand: CardBrandChoice,
        val paymentMethodIcon: Int,
        val shouldShowCardBrandDropdown: Boolean,
        val availableNetworks: List<CardBrandChoice>
    )

    sealed interface ViewAction {
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction
    }

    companion object {
        fun create(
            coroutineScope: CoroutineScope,
            isModifiable: Boolean,
            cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
            card: PaymentMethod.Card,
            onBrandChoiceChanged: CardBrandCallback,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor {
            return DefaultEditCardDetailsInteractor(
                card = card,
                cardBrandFilter = cardBrandFilter,
                coroutineScope = coroutineScope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardUpdateParamsChanged = onCardUpdateParamsChanged,
                isModifiable = isModifiable
            )
        }
    }
}

private class DefaultEditCardDetailsInteractor(
    private val card: PaymentMethod.Card,
    private val cardBrandFilter: CardBrandFilter,
    private val isModifiable: Boolean,
    coroutineScope: CoroutineScope,
    private val onBrandChoiceChanged: CardBrandCallback,
    override val onCardUpdateParamsChanged: CardUpdateParamsCallback
) : EditCardDetailsInteractor {
    private val cardDetailsEntry = MutableStateFlow(
        value = buildDefaultCardEntry()
    )

    override val state: StateFlow<EditCardDetailsInteractor.State> = cardDetailsEntry.mapLatest { inputState ->
        uiState(inputState.cardBrandChoice)
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = uiState()
    )

    init {
        coroutineScope.launch(Dispatchers.Main) {
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
}
