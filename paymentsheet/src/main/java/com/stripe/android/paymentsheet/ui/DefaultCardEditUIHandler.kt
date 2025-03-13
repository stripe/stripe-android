package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.CardDetailsUtil
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

internal class DefaultCardEditUIHandler(
    override val card: PaymentMethod.Card,
    override val billingDetails: PaymentMethod.BillingDetails?,
    override val addressCollectionMode: AddressCollectionMode,
    override val cardBrandFilter: CardBrandFilter,
    private val scope: CoroutineScope,
    override val onBrandChoiceOptionsShown: (CardBrand) -> Unit,
    override val onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
    override val onCardValuesChanged: (CardUpdateParams?) -> Unit,
) : CardEditUIHandler {
    private val _cardInputState = MutableStateFlow(
        value = CardInputState(
            card = card,
            cardBrandChoice = defaultCardBrandChoice(),
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode,
            entry = buildDefaultCardEntry()
        )
    )

    private val dateConfig = DateConfig()
    private val cardBillingAddressElement: CardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        rawValuesMap = rawAddressValues()
    )

    override val expDate = formattedExpiryDate()

    override val addressSectionElement = SectionElement.wrap(cardBillingAddressElement)

    override val collectAddress = addressCollectionMode != AddressCollectionMode.Never

    override val hiddenAddressElements = buildHiddenAddressElements()

    override val state: StateFlow<CardEditUIHandler.State> = _cardInputState.mapLatest { inputState ->
        CardEditUIHandler.State(
            card = inputState.card,
            expDate = formattedExpiryDate(),
            addressElement = addressSectionElement,
            hiddenAddressFields = hiddenAddressElements,
            collectAddress = collectAddress,
            selectedCardBrand = inputState.entry.cardBrandChoice
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = defaultState()
    )


    init {
        scope.launch {
            listenToForm()
        }

        scope.launch {
            _cardInputState.collectLatest { state ->
                println("TOLUWANI => entry.isValid: ${state.valid}, entry.addressValid: ${state.addressValid()}, entry.expDateValid: ${state.expDateValid()}")
                val newParams = state.takeIf {
                    it.hasChanged && it.valid
                }?.entry?.toUpdateParams(collectAddress)
                onCardValuesChanged(newParams)
            }
        }
    }

    private fun defaultState(): CardEditUIHandler.State {
        return CardEditUIHandler.State(
            card = card,
            expDate = expDate,
            addressElement = addressSectionElement,
            hiddenAddressFields = hiddenAddressElements,
            collectAddress = collectAddress,
            selectedCardBrand = defaultCardBrandChoice()
        )
    }

    private fun defaultCardBrandChoice() = card.getPreferredChoice(cardBrandFilter)

    private fun buildDefaultCardEntry(): CardDetailsEntry {
        val entry = CardDetailsEntry(
            cardBrandChoice = card.getPreferredChoice(cardBrandFilter),
            expMonth = card.expiryMonth,
            expYear = card.expiryYear
        )
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                entry.copy(
                    country = billingDetails?.address?.country?.toFormFieldEntry(),
                    postalCode = billingDetails?.address?.postalCode?.toFormFieldEntry()
                )
            }
            AddressCollectionMode.Never -> {
                entry
            }
            AddressCollectionMode.Full -> {
                entry.copy(
                    line1 = billingDetails?.address?.line1?.toFormFieldEntry(),
                    line2 = billingDetails?.address?.line2?.toFormFieldEntry(),
                    city = billingDetails?.address?.city?.toFormFieldEntry(),
                    state = billingDetails?.address?.state?.toFormFieldEntry(),
                    country = billingDetails?.address?.country?.toFormFieldEntry(),
                    postalCode = billingDetails?.address?.postalCode?.toFormFieldEntry()
                )
            }
        }
    }

    override fun dateChanged(text: String) {
        val map = CardDetailsUtil.createExpiryDateFormFieldValues(FormFieldEntry(text))
        _cardInputState.update {
            val entry = it.entry
            it.copy(
                entry = entry.copy(
                    expYear = map[IdentifierSpec.CardExpYear]?.value?.toIntOrNull()?.takeIf { it > 0 },
                    expMonth = map[IdentifierSpec.CardExpMonth]?.value?.toIntOrNull()?.takeIf { it > 0 },
                )
            )
        }
    }

    private suspend fun listenToForm() {
        cardBillingAddressElement.getFormFieldValueFlow()
            .collectLatest { field ->
                // What is the IdentifierSpec.OneLineAddress
                val line1 = field.valueOrNull(IdentifierSpec.Line1)
                val line2 = field.valueOrNull(IdentifierSpec.Line2)
                val city = field.valueOrNull(IdentifierSpec.City)
                val postalCode = field.valueOrNull(IdentifierSpec.PostalCode)
                val country = field.valueOrNull(IdentifierSpec.Country)
                val state = field.valueOrNull(IdentifierSpec.State)
                _cardInputState.update {
                    val entry = it.entry
                    it.copy(
                        entry = entry.copy(
                            line1 = line1,
                            line2 = line2,
                            city = city,
                            postalCode = postalCode,
                            country = country,
                            state = state
                        )
                    )
                }
            }
    }

    private fun List<Pair<IdentifierSpec, FormFieldEntry>>.valueOrNull(
        identifierSpec: IdentifierSpec
    ): FormFieldEntry? {
        return firstOrNull {
            it.first == identifierSpec
        }?.second
    }

    private fun formattedExpiryDate(): String {
        cardBillingAddressElement.getFormFieldValueFlow()
        val expiryMonth = card.expiryMonth
        val expiryYear = card.expiryYear
        if (expiryMonth == null || expiryYear == null) return ""
        val formattedExpiryMonth = if (expiryMonth < Calendar.OCTOBER) {
            "0$expiryMonth"
        } else {
            expiryMonth.toString()
        }

        @Suppress("MagicNumber")
        val formattedExpiryYear = expiryYear.toString().substring(2, 4)

        return "$formattedExpiryMonth$formattedExpiryYear"
    }

    private fun buildHiddenAddressElements(): Set<IdentifierSpec> {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                return setOf(
                    IdentifierSpec.Line1,
                    IdentifierSpec.Line2,
                    IdentifierSpec.City,
                    IdentifierSpec.State,
                )
            }
            AddressCollectionMode.Never -> emptySet()
            AddressCollectionMode.Full -> emptySet()
        }
    }

    override fun dateValidator(text: String): TextFieldState {
        return dateConfig.determineState(text)
    }

    override fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice) {
        _cardInputState.update {
            val entry = it.entry
            it.copy(
                entry = entry.copy(
                    cardBrandChoice = cardBrandChoice
                )
            )
        }
    }

    override fun onBrandChoiceOptionsDismissed() {
        onBrandChoiceOptionsDismissed(_cardInputState.value.entry.cardBrandChoice.brand)
    }

    override fun onBrandChoiceOptionsShown() {
        onBrandChoiceOptionsShown(_cardInputState.value.entry.cardBrandChoice.brand)
    }

    private fun rawAddressValues(): Map<IdentifierSpec, String?> {
        val address = billingDetails?.address ?: return emptyMap()
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                mapOf(
                    IdentifierSpec.Country to address.country,
                    IdentifierSpec.PostalCode to address.postalCode
                )
            }
            AddressCollectionMode.Never -> emptyMap()
            AddressCollectionMode.Full -> {
                mapOf(
                    IdentifierSpec.Line1 to address.line1,
                    IdentifierSpec.Line2 to address.line2,
                    IdentifierSpec.State to address.state,
                    IdentifierSpec.City to address.city,
                    IdentifierSpec.Country to address.country,
                    IdentifierSpec.PostalCode to address.postalCode
                )
            }
        }
    }

    class Factory(
        private val scope: CoroutineScope,
    ) : CardEditUIHandler.Factory {
        override fun create(
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
            addressCollectionMode: AddressCollectionMode,
            cardBrandFilter: CardBrandFilter,
            onBrandChoiceOptionsShown: (CardBrand) -> Unit,
            onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
            onCardValuesChanged: (CardUpdateParams?) -> Unit
        ): CardEditUIHandler {
            return DefaultCardEditUIHandler(
                card = card,
                billingDetails = billingDetails,
                addressCollectionMode = addressCollectionMode,
                cardBrandFilter = cardBrandFilter,
                scope = scope,
                onBrandChoiceOptionsShown = onBrandChoiceOptionsShown,
                onBrandChoiceOptionsDismissed = onBrandChoiceOptionsDismissed,
                onCardValuesChanged = onCardValuesChanged
            )
        }

    }

    companion object {
        fun factory(
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
            addressCollectionMode: AddressCollectionMode,
            cardBrandFilter: CardBrandFilter,
            scope: CoroutineScope,
            onBrandChoiceOptionsShown: (CardBrand) -> Unit,
            onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
            onCardValuesChanged: (CardUpdateParams?) -> Unit
        ): DefaultCardEditUIHandler {
            return DefaultCardEditUIHandler(
                card = card,
                billingDetails = billingDetails,
                addressCollectionMode = addressCollectionMode,
                cardBrandFilter = cardBrandFilter,
                scope = scope,
                onBrandChoiceOptionsShown = onBrandChoiceOptionsShown,
                onBrandChoiceOptionsDismissed = onBrandChoiceOptionsDismissed,
                onCardValuesChanged = onCardValuesChanged
            )
        }
    }
}

private fun String.toFormFieldEntry() = FormFieldEntry(this, isComplete = true)