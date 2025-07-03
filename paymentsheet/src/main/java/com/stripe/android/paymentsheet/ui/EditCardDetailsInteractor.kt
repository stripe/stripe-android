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
import com.stripe.android.uicore.elements.SectionFieldElement
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
            details: ConsumerPaymentDetails.PaymentDetails,
            billingPhoneNumber: String?,
        ): EditCardPayload {
            val cardPaymentDetails = details as? ConsumerPaymentDetails.Card
            return EditCardPayload(
                last4 = cardPaymentDetails?.last4,
                expiryMonth = cardPaymentDetails?.expiryMonth,
                expiryYear = cardPaymentDetails?.expiryYear,
                brand = cardPaymentDetails?.brand ?: Unknown,
                displayBrand = cardPaymentDetails?.brand?.code,
                networks = cardPaymentDetails?.networks?.toSet().takeIf { it?.size != null && it.size > 1 },
                billingDetails = PaymentMethod.BillingDetails(
                    address = details.billingAddress?.let {
                        Address(
                            line1 = it.line1,
                            line2 = it.line2,
                            city = it.locality,
                            state = it.administrativeArea,
                            postalCode = it.postalCode,
                            country = it.countryCode?.value,
                        )
                    },
                    email = details.billingEmailAddress,
                    name = details.billingAddress?.name,
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

internal data class CardEditConfiguration(
    val cardBrandFilter: CardBrandFilter,
    val isCbcModifiable: Boolean,
    // Local flag for whether expiry date and address can be edited.
    // This flag has no effect on Card Brand Choice.
    // It will be removed before release.
    val areExpiryDateAndAddressModificationSupported: Boolean,
)

internal interface EditCardDetailsInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    @Immutable
    data class State(
        val payload: EditCardPayload,
        val paymentMethodIcon: Int,
        val cardDetailsState: CardDetailsState?,
        val billingDetailsForm: BillingDetailsForm?,
    ) {
        val contactSectionElements: List<SectionFieldElement>
            get() = buildList {
                val hasCardSection = cardDetailsState != null

                // Name goes in contact section only if there's NO card section
                if (!hasCardSection && billingDetailsForm?.nameElement != null) {
                    add(billingDetailsForm.nameElement)
                }
                billingDetailsForm?.emailElement?.let { add(it) }
                billingDetailsForm?.phoneElement?.let { add(it) }
            }

        val nameElementForCardSection: SectionFieldElement?
            get() {
                // Name goes in card section if there's a card section
                return if (cardDetailsState != null) {
                    billingDetailsForm?.nameElement
                } else {
                    null
                }
            }

        val needsSpacerBeforeBilling: Boolean
            get() = (contactSectionElements.isNotEmpty() || cardDetailsState != null) && billingDetailsForm != null
    }

    @Immutable
    data class CardDetailsState(
        val selectedCardBrand: CardBrandChoice,
        val shouldShowCardBrandDropdown: Boolean,
        val availableNetworks: List<CardBrandChoice>,
        val expiryDateState: ExpiryDateState,
        val billingDetailsForm: BillingDetailsForm?,
        val requireModification: Boolean = true,
    )

    sealed interface ViewAction {
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction
        data class DateChanged(val text: String) : ViewAction
        data class BillingDetailsChanged(val billingDetailsFormState: BillingDetailsFormState) : ViewAction
    }

    fun interface Factory {
        /**
         * Creates an instance of [EditCardDetailsInteractor].
         *
         * @param coroutineScope The [CoroutineScope] in which the interactor will operate.
         * @param cardEditConfiguration Optional configuration for editing card details.
         *        If null, card editing is not supported.
         * @param payload The [EditCardPayload] containing the initial form details.
         * @param billingDetailsCollectionConfiguration Configuration for billing details collection.
         *       Depending on this configuration, the interactor may or may not collect billing details.
         * @param onBrandChoiceChanged Callback invoked when the card brand choice changes.
         * @param onCardUpdateParamsChanged Callback invoked when the card update parameters change.
         */
        fun create(
            coroutineScope: CoroutineScope,
            cardEditConfiguration: CardEditConfiguration?,
            requiresModification: Boolean,
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
    private val cardEditConfiguration: CardEditConfiguration?,
    // Whether the card details require modification to be submitted.
    // on scenarios where we prefill details, we just want the form to be completed,
    // not necessarily user-modified.
    private val requiresModification: Boolean,
    private val coroutineScope: CoroutineScope,
    private val onBrandChoiceChanged: CardBrandCallback,
    private val onCardUpdateParamsChanged: CardUpdateParamsCallback
) : EditCardDetailsInteractor {
    private val cardDetailsEntry = MutableStateFlow(
        value = cardEditConfiguration?.buildDefaultCardEntry()
    )
    private val billingDetailsEntry = MutableStateFlow<BillingDetailsEntry?>(null)
    private val billingDetailsForm = defaultBillingDetailsForm()

    override val state: StateFlow<EditCardDetailsInteractor.State> = cardDetailsEntry.mapLatest { inputState ->
        uiState(
            cardDetailsEntry = inputState,
            billingDetailsForm = billingDetailsForm
        )
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = uiState(
            cardDetailsEntry = cardDetailsEntry.value,
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
        cardDetailsEntry: CardDetailsEntry?,
        billingDetailsEntry: BillingDetailsEntry?
    ): CardUpdateParams? {
        val hasChanges = hasCardDetailsChanged(cardDetailsEntry) ||
            hasBillingDetailsChanged(billingDetailsEntry)
        val isComplete = (cardDetailsEntry?.isComplete() != false) &&
            billingDetailsEntry?.isComplete(billingDetailsCollectionConfiguration) != false

        return if ((hasChanges || requiresModification.not()) && isComplete) {
            toUpdateParams(cardDetailsEntry, billingDetailsEntry)
        } else {
            null
        }
    }

    private fun hasCardDetailsChanged(cardDetailsEntry: CardDetailsEntry?): Boolean {
        return if (cardEditConfiguration != null && cardDetailsEntry != null) {
            cardDetailsEntry.hasChanged(
                editCardPayload = payload,
                originalCardBrandChoice = cardEditConfiguration.defaultCardBrandChoice(),
            )
        } else {
            false
        }
    }

    private fun hasBillingDetailsChanged(billingDetailsEntry: BillingDetailsEntry?): Boolean {
        return billingDetailsEntry?.hasChanged(
            billingDetails = payload.billingDetails,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        ) ?: false
    }

    private fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        if (cardEditConfiguration != null) {
            val currentCardBrand = state.value.cardDetailsState?.selectedCardBrand
            if (cardBrandChoice != currentCardBrand) {
                onBrandChoiceChanged(cardBrandChoice.brand)
            }
            cardDetailsEntry.update { entry ->
                entry?.copy(
                    cardBrandChoice = cardBrandChoice
                )
            }
        }
    }

    private fun onDateChanged(text: String) {
        if (cardEditConfiguration != null) {
            cardDetailsEntry.update { entry ->
                entry?.copy(
                    expiryDateState = entry.expiryDateState.onDateChanged(text),
                )
            }
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

    private fun CardEditConfiguration.buildDefaultCardEntry(): CardDetailsEntry {
        return CardDetailsEntry(
            cardBrandChoice = defaultCardBrandChoice(),
            expiryDateState = defaultExpiryDateState(),
        )
    }

    private fun CardEditConfiguration.defaultCardBrandChoice(): CardBrandChoice {
        return if (cardEditConfiguration != null) {
            payload.getPreferredChoice(cardEditConfiguration.cardBrandFilter)
        } else {
            CardBrandChoice(Unknown, enabled = false)
        }
    }

    private fun CardEditConfiguration.defaultExpiryDateState(): ExpiryDateState {
        return ExpiryDateState.create(
            editPayload = payload,
            enabled = areExpiryDateAndAddressModificationSupported == true
        )
    }

    private fun defaultBillingDetailsForm(): BillingDetailsForm? {
        val showAddressForm = (cardEditConfiguration?.areExpiryDateAndAddressModificationSupported ?: true) &&
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
        cardDetailsEntry: CardDetailsEntry?,
        billingDetailsForm: BillingDetailsForm?,
    ): EditCardDetailsInteractor.State {
        return EditCardDetailsInteractor.State(
            payload = payload,
            paymentMethodIcon = payload.getSavedPaymentMethodIcon(forVerticalMode = true),
            cardDetailsState = if (cardEditConfiguration != null && cardDetailsEntry != null) {
                EditCardDetailsInteractor.CardDetailsState(
                    selectedCardBrand = cardDetailsEntry.cardBrandChoice,
                    shouldShowCardBrandDropdown = cardEditConfiguration.isCbcModifiable,
                    availableNetworks = payload.getAvailableNetworks(cardEditConfiguration.cardBrandFilter),
                    expiryDateState = cardDetailsEntry.expiryDateState,
                )
            } else {
                null
            },
            billingDetailsForm = billingDetailsForm,
        )
    }

    class Factory : EditCardDetailsInteractor.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            cardEditConfiguration: CardEditConfiguration?,
            requiresModification: Boolean,
            payload: EditCardPayload,
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
            onBrandChoiceChanged: CardBrandCallback,
            onCardUpdateParamsChanged: CardUpdateParamsCallback
        ): EditCardDetailsInteractor {
            return DefaultEditCardDetailsInteractor(
                payload = payload,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                cardEditConfiguration = cardEditConfiguration,
                coroutineScope = coroutineScope,
                onBrandChoiceChanged = onBrandChoiceChanged,
                onCardUpdateParamsChanged = onCardUpdateParamsChanged,
                requiresModification = requiresModification
            )
        }
    }
}
