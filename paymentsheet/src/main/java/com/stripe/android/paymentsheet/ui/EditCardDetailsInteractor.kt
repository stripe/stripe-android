package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.CardBrandFilter
import com.stripe.android.core.utils.DateUtils
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
        val availableNetworks: List<CardBrandChoice>,
        val expiryDateState: ExpiryDateState,
        val billingDetailsForm: BillingDetailsForm? = null
    )

    sealed interface ViewAction {
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction
        data class DateChanged(val text: String) : ViewAction
        data class BillingDetailsChanged(val billingDetailsFormState: BillingDetailsFormState) : ViewAction
    }

    fun interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            isModifiable: Boolean,
            areExpiryDateAndAddressModificationSupported: Boolean,
            cardBrandFilter: CardBrandFilter,
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
            addressCollectionMode: AddressCollectionMode,
            onBrandChoiceChanged: CardBrandCallback,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor
    }
}

internal class DefaultEditCardDetailsInteractor(
    private val card: PaymentMethod.Card,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
    private val cardBrandFilter: CardBrandFilter,
    private val isModifiable: Boolean,
    // Local flag for whether expiry date and address can be edited.
    // This flag has no effect on Card Brand Choice.
    // It will be removed before release.
    private val areExpiryDateAndAddressModificationSupported: Boolean,
    private val coroutineScope: CoroutineScope,
    private val onBrandChoiceChanged: CardBrandCallback,
    private val onCardUpdateParamsChanged: CardUpdateParamsCallback
) : EditCardDetailsInteractor {
    private val cardDetailsEntry = MutableStateFlow(
        value = buildDefaultCardEntry()
    )
    private val billingDetailsEntry = MutableStateFlow<BillingDetailsEntry?>(null)
    private val billingDetailsForm = defaultBillingDetailsForm()

    override val state: StateFlow<EditCardDetailsInteractor.State> = cardDetailsEntry.mapLatest { inputState ->
        uiState(
            cardBrandChoice = inputState.cardBrandChoice,
            expiryDateState = inputState.expiryDateState,
            billingDetailsForm = billingDetailsForm
        )
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = uiState(
            cardBrandChoice = cardDetailsEntry.value.cardBrandChoice,
            expiryDateState = cardDetailsEntry.value.expiryDateState,
            billingDetailsForm = billingDetailsForm
        )
    )

    init {
        coroutineScope.launch(Dispatchers.Main) {
            combine(
                flow = cardDetailsEntry,
                flow2 = billingDetailsEntry
            ) { cardDetailsEntry, billingDetailsEntry ->
                newCardUpdateParams(cardDetailsEntry, billingDetailsEntry)
            }.collectLatest { newParams ->
                onCardUpdateParamsChanged(newParams)
            }
        }
    }

    private fun newCardUpdateParams(
        cardDetailsEntry: CardDetailsEntry,
        billingDetailsEntry: BillingDetailsEntry?
    ): CardUpdateParams? {
        val shouldEmitCardDetailsUpdate = shouldEmitCardDetailsUpdate(cardDetailsEntry)
        val shouldEmitBillingDetailsUpdate = shouldEmitBillingDetailsUpdate(billingDetailsEntry)
        val shouldTakeUpdate = shouldEmitCardDetailsUpdate || shouldEmitBillingDetailsUpdate
        return if (shouldTakeUpdate) {
            cardDetailsEntry.toUpdateParams(billingDetailsEntry)
        } else {
            null
        }
    }

    private fun shouldEmitCardDetailsUpdate(cardDetailsEntry: CardDetailsEntry): Boolean {
        val hasChanged = cardDetailsEntry.hasChanged(
            card = card,
            originalCardBrandChoice = defaultCardBrandChoice(),
        )
        return hasChanged && cardDetailsEntry.isComplete()
    }

    private fun shouldEmitBillingDetailsUpdate(billingDetailsEntry: BillingDetailsEntry?): Boolean {
        val hasChanged = billingDetailsEntry?.hasChanged(
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode
        ) ?: false
        val isComplete = billingDetailsEntry?.isComplete(addressCollectionMode) ?: false
        return hasChanged && isComplete
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
        cardDetailsEntry.update { entry ->
            entry.copy(
                expiryDateState = entry.expiryDateState.onDateChanged(text),
            )
        }
    }

    private fun onBillingAddressFormChanged(state: BillingDetailsFormState) {
        billingDetailsEntry.value = BillingDetailsEntry(
            billingDetailsFormState = state,
        )
    }

    override fun handleViewAction(viewAction: EditCardDetailsInteractor.ViewAction) {
        when (viewAction) {
            is EditCardDetailsInteractor.ViewAction.BrandChoiceChanged -> {
                onBrandChoiceChanged(viewAction.cardBrandChoice)
            }
            is EditCardDetailsInteractor.ViewAction.DateChanged -> {
                onDateChanged(viewAction.text)
            }
            is EditCardDetailsInteractor.ViewAction.BillingDetailsChanged -> {
                onBillingAddressFormChanged(viewAction.billingDetailsFormState)
            }
        }
    }

    private fun buildDefaultCardEntry(): CardDetailsEntry {
        return CardDetailsEntry(
            cardBrandChoice = defaultCardBrandChoice(),
            expiryDateState = defaultExpiryDateState(),
        )
    }

    private fun defaultCardBrandChoice() = card.getPreferredChoice(cardBrandFilter)

    private fun defaultExpiryDateState(): ExpiryDateState {
        return ExpiryDateState.create(
            card = card,
            enabled = areExpiryDateAndAddressModificationSupported
        )
    }

    private fun defaultBillingDetailsForm(): BillingDetailsForm? {
        val showAddressForm = areExpiryDateAndAddressModificationSupported &&
            addressCollectionMode != AddressCollectionMode.Never
        if (showAddressForm.not()) {
            return null
        }
        return BillingDetailsForm(
            addressCollectionMode = addressCollectionMode,
            billingDetails = billingDetails
        )
    }

    private fun uiState(
        cardBrandChoice: CardBrandChoice,
        expiryDateState: ExpiryDateState,
        billingDetailsForm: BillingDetailsForm?
    ): EditCardDetailsInteractor.State {
        return EditCardDetailsInteractor.State(
            card = card,
            selectedCardBrand = cardBrandChoice,
            paymentMethodIcon = card.getSavedPaymentMethodIcon(forVerticalMode = true),
            shouldShowCardBrandDropdown = isModifiable && isExpired().not(),
            availableNetworks = card.getAvailableNetworks(cardBrandFilter),
            expiryDateState = expiryDateState,
            billingDetailsForm = billingDetailsForm
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
            areExpiryDateAndAddressModificationSupported: Boolean,
            cardBrandFilter: CardBrandFilter,
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
            addressCollectionMode: AddressCollectionMode,
            onBrandChoiceChanged: CardBrandCallback,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor {
            return DefaultEditCardDetailsInteractor(
                card = card,
                cardBrandFilter = cardBrandFilter,
                isModifiable = isModifiable,
                coroutineScope = coroutineScope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardUpdateParamsChanged = onCardUpdateParamsChanged,
                areExpiryDateAndAddressModificationSupported = areExpiryDateAndAddressModificationSupported,
                billingDetails = billingDetails,
                addressCollectionMode = addressCollectionMode
            )
        }
    }
}
