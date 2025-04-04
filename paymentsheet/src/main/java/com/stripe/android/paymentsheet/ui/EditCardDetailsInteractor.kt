package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.CardBrandFilter
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.stripe.android.ui.core.elements.CardDetailsUtil
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.forms.FormFieldEntry
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

    fun handleViewAction(viewAction: ViewAction)

    @Immutable
    data class State(
        val card: PaymentMethod.Card,
        val selectedCardBrand: CardBrandChoice,
        val paymentMethodIcon: Int,
        val shouldShowCardBrandDropdown: Boolean,
        val shouldAllowExpDateEdit: Boolean,
        val availableNetworks: List<CardBrandChoice>,
        val dateValidator: (String) -> TextFieldState
    )

    sealed interface ViewAction {
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction
        data class DateChanged(val text: String) : ViewAction
    }

    fun interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            isModifiable: Boolean,
            isCardDetailEditSupported: Boolean,
            cardBrandFilter: CardBrandFilter,
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
                isModifiable = isModifiable,
                isCardDetailEditSupported = isCardDetailEditSupported
            )
        }
    }
}

internal class DefaultEditCardDetailsInteractor(
    private val card: PaymentMethod.Card,
    private val cardBrandFilter: CardBrandFilter,
    private val isModifiable: Boolean,
    private val isCardDetailEditSupported: Boolean,
    coroutineScope: CoroutineScope,
    private val onBrandChoiceChanged: CardBrandCallback,
    private val onCardUpdateParamsChanged: CardUpdateParamsCallback
) : EditCardDetailsInteractor {
    private val dateConfig = DateConfig()
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
                    val hasChanged = it.hasChanged(
                        card = card,
                        originalCardBrandChoice = defaultCardBrandChoice(),
                    )
                    val isComplete = it.isComplete(expiryDateEditable = isCardDetailEditSupported)
                    hasChanged && isComplete
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

    private fun onDateChanged(text: String) {
        val isValid = dateConfig.determineState(text).isValid()
        if (isValid.not()) {
            cardDetailsEntry.update { entry ->
                entry.copy(
                    expYear = null,
                    expMonth = null,
                )
            }
            return
        }

        val map = CardDetailsUtil.createExpiryDateFormFieldValues(FormFieldEntry(text))
        cardDetailsEntry.update { entry ->
            entry.copy(
                expYear = map[IdentifierSpec.CardExpYear]?.value?.toIntOrNull()?.takeIf { it > 0 },
                expMonth = map[IdentifierSpec.CardExpMonth]?.value?.toIntOrNull()?.takeIf { it > 0 },
            )
        }
    }

    override fun handleViewAction(viewAction: EditCardDetailsInteractor.ViewAction) {
        when (viewAction) {
            is EditCardDetailsInteractor.ViewAction.BrandChoiceChanged -> {
                onBrandChoiceChanged(viewAction.cardBrandChoice)
            }
            is EditCardDetailsInteractor.ViewAction.DateChanged -> {
                onDateChanged(viewAction.text)
            }
        }
    }

    private fun buildDefaultCardEntry(): CardDetailsEntry {
        return CardDetailsEntry(
            cardBrandChoice = defaultCardBrandChoice(),
            expMonth = card.expiryMonth,
            expYear = card.expiryYear
        )
    }

    private fun defaultCardBrandChoice() = card.getPreferredChoice(cardBrandFilter)

    private fun uiState(cardBrandChoice: CardBrandChoice = defaultCardBrandChoice()): EditCardDetailsInteractor.State {
        return EditCardDetailsInteractor.State(
            card = card,
            selectedCardBrand = cardBrandChoice,
            paymentMethodIcon = card.getSavedPaymentMethodIcon(forVerticalMode = true),
            shouldShowCardBrandDropdown = isModifiable && isExpired().not(),
            shouldAllowExpDateEdit = isCardDetailEditSupported,
            availableNetworks = card.getAvailableNetworks(cardBrandFilter),
            dateValidator = { date ->
                dateConfig.determineState(date)
            }
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

    class Factory : EditCardDetailsInteractor.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            isModifiable: Boolean,
            isCardDetailEditSupported: Boolean,
            cardBrandFilter: CardBrandFilter,
            card: PaymentMethod.Card,
            onBrandChoiceChanged: CardBrandCallback,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor {
            return DefaultEditCardDetailsInteractor(
                card = card,
                cardBrandFilter = cardBrandFilter,
                isModifiable = isModifiable,
                coroutineScope = coroutineScope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardUpdateParamsChanged = onCardUpdateParamsChanged
            )
        }
    }
}
