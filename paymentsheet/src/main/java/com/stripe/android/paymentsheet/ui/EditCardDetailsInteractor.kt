package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardBrand.Unknown
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
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

internal data class EditCardPayload(
    val last4: String?,
    val expiryMonth: Int?,
    val expiryYear: Int?,
    val brand: CardBrand,
    val displayBrand: String?,
    val networks: Set<String>?,
    val billingDetails: PaymentMethod.BillingDetails?,
) {

    val cardBrand: CardBrand
        get() = CardBrand.fromCode(displayBrand).takeIf { it != Unknown } ?: brand

    internal companion object {

        fun create(
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
        ): EditCardPayload {
            return EditCardPayload(
                last4 = card.last4,
                expiryMonth = card.expiryMonth,
                expiryYear = card.expiryYear,
                brand = card.brand,
                displayBrand = card.displayBrand,
                networks = card.networks?.available,
                billingDetails = billingDetails,
            )
        }

        fun create(
            card: ConsumerPaymentDetails.Card,
            billingPhoneNumber: String?,
        ): EditCardPayload {
            return EditCardPayload(
                last4 = card.last4,
                expiryMonth = card.expiryMonth,
                expiryYear = card.expiryYear,
                brand = card.brand,
                displayBrand = card.brand.code,
                networks = card.networks.toSet().takeIf { it.size > 1 },
                billingDetails = PaymentMethod.BillingDetails(
                    address = card.billingAddress?.let {
                        Address(
                            line1 = it.line1,
                            line2 = it.line2,
                            city = it.locality,
                            state = it.administrativeArea,
                            postalCode = it.postalCode,
                            country = it.countryCode?.value,
                        )
                    },
                    email = card.billingEmailAddress,
                    name = card.billingAddress?.name,
                    phone = billingPhoneNumber,
                )
            )
        }

        fun create(link: LinkPaymentDetails.Card): EditCardPayload {
            return EditCardPayload(
                last4 = link.last4,
                expiryMonth = link.expMonth,
                expiryYear = link.expYear,
                brand = link.brand,
                displayBrand = null,
                networks = null,
                billingDetails = null,
            )
        }
    }
}

internal interface EditCardDetailsInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    @Immutable
    data class State(
        val payload: EditCardPayload,
        val selectedCardBrand: CardBrandChoice,
        val paymentMethodIcon: Int,
        val shouldShowCardBrandDropdown: Boolean,
        val availableNetworks: List<CardBrandChoice>,
        val expiryDateState: ExpiryDateState,
        val billingDetailsForm: BillingDetailsForm?,
    )

    sealed interface ViewAction {
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction
        data class DateChanged(val text: String) : ViewAction
        data class BillingDetailsChanged(val billingDetailsFormState: BillingDetailsFormState) : ViewAction
    }

    fun interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            isCbcModifiable: Boolean,
            areExpiryDateAndAddressModificationSupported: Boolean,
            cardBrandFilter: CardBrandFilter,
            payload: EditCardPayload,
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
            onBrandChoiceChanged: CardBrandCallback,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor
    }
}

internal class DefaultEditCardDetailsInteractor(
    private val payload: EditCardPayload,
    private val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
    private val cardBrandFilter: CardBrandFilter,
    private val isCbcModifiable: Boolean,
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
        val hasChanges = hasCardDetailsChanged(cardDetailsEntry) ||
            hasBillingDetailsChanged(billingDetailsEntry)
        val isComplete = cardDetailsEntry.isComplete() &&
            billingDetailsEntry?.isComplete(billingDetailsCollectionConfiguration) != false

        return if (hasChanges && isComplete) {
            toUpdateParams(cardDetailsEntry, billingDetailsEntry)
        } else {
            null
        }
    }

    private fun hasCardDetailsChanged(cardDetailsEntry: CardDetailsEntry): Boolean {
        return cardDetailsEntry.hasChanged(
            editCardPayload = payload,
            originalCardBrandChoice = defaultCardBrandChoice(),
        )
    }

    private fun hasBillingDetailsChanged(billingDetailsEntry: BillingDetailsEntry?): Boolean {
        return billingDetailsEntry?.hasChanged(
            billingDetails = payload.billingDetails,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        ) ?: false
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

    private fun defaultCardBrandChoice() = payload.getPreferredChoice(cardBrandFilter)

    private fun defaultExpiryDateState(): ExpiryDateState {
        return ExpiryDateState.create(
            editPayload = payload,
            enabled = areExpiryDateAndAddressModificationSupported
        )
    }

    private fun defaultBillingDetailsForm(): BillingDetailsForm? {
        val showAddressForm = areExpiryDateAndAddressModificationSupported &&
            billingDetailsCollectionConfiguration.address != AddressCollectionMode.Never
        val collectsContactDetails = billingDetailsCollectionConfiguration.collectsName ||
            billingDetailsCollectionConfiguration.collectsEmail ||
            billingDetailsCollectionConfiguration.collectsPhone

        if (showAddressForm.not() && collectsContactDetails.not()) {
            return null
        }
        return BillingDetailsForm(
            addressCollectionMode = billingDetailsCollectionConfiguration.address,
            billingDetails = payload.billingDetails,
            collectName = billingDetailsCollectionConfiguration.collectsName,
            collectEmail = billingDetailsCollectionConfiguration.collectsEmail,
            collectPhone = billingDetailsCollectionConfiguration.collectsPhone,
        )
    }

    private fun uiState(
        cardBrandChoice: CardBrandChoice,
        expiryDateState: ExpiryDateState,
        billingDetailsForm: BillingDetailsForm?,
    ): EditCardDetailsInteractor.State {
        return EditCardDetailsInteractor.State(
            payload = payload,
            selectedCardBrand = cardBrandChoice,
            paymentMethodIcon = payload.getSavedPaymentMethodIcon(forVerticalMode = true),
            shouldShowCardBrandDropdown = isCbcModifiable,
            availableNetworks = payload.getAvailableNetworks(cardBrandFilter),
            expiryDateState = expiryDateState,
            billingDetailsForm = billingDetailsForm,
        )
    }

    class Factory : EditCardDetailsInteractor.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            isCbcModifiable: Boolean,
            areExpiryDateAndAddressModificationSupported: Boolean,
            cardBrandFilter: CardBrandFilter,
            payload: EditCardPayload,
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
            onBrandChoiceChanged: CardBrandCallback,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor {
            return DefaultEditCardDetailsInteractor(
                payload = payload,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                cardBrandFilter = cardBrandFilter,
                isCbcModifiable = isCbcModifiable,
                coroutineScope = coroutineScope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardUpdateParamsChanged = onCardUpdateParamsChanged,
                areExpiryDateAndAddressModificationSupported = areExpiryDateAndAddressModificationSupported
            )
        }
    }
}
