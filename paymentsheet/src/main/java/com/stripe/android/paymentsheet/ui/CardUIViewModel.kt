package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.Address
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar.OCTOBER

internal class CardUIViewModel(
    private val card: PaymentMethod.Card,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
    private val cardBrandFilter: CardBrandFilter,
    private val onBrandChoiceOptionsShown: (CardBrand) -> Unit,
    private val onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
) : ViewModel() {
    private val _cardInputState = MutableStateFlow(
        value = CardInputState(
            card = card,
            cardBrandChoice = defaultCardBrandChoice(),
            billingDetails = billingDetails,
            addressCollectionMode = addressCollectionMode,
            entry = buildDefaultCardEntry()
        )
    )
    val cardInputState: Flow<CardUpdateParams?> = _cardInputState.mapLatest { state ->
        state.takeIf { it.hasChanged }?.entry?.toUpdateParams(collectAddress)
    }
    private val dateConfig = DateConfig()
    private val cardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        rawValuesMap = billingDetails?.address?.let {
            mapOf(
                IdentifierSpec.Line1 to it.line1,
                IdentifierSpec.Line2 to it.line2,
                IdentifierSpec.State to it.state,
                IdentifierSpec.City to it.city,
                IdentifierSpec.Country to it.country,
                IdentifierSpec.PostalCode to it.postalCode
            )
        } ?: emptyMap()
    )

    val expDate = formattedExpiryDate()

    val addressSectionElement = SectionElement.wrap(cardBillingAddressElement)

    val collectAddress = addressCollectionMode != AddressCollectionMode.Never

    val selectedCardBrandFlow: Flow<CardBrandChoice> = _cardInputState.mapLatest { it.entry.cardBrandChoice }

    val hiddenAddressElements = buildHiddenAddressElements()

    init {
        viewModelScope.launch {
            listenToForm()
        }
    }

    fun defaultCardBrandChoice() = card.getPreferredChoice(cardBrandFilter)

    fun handleViewAction(viewAction: ViewAction) {
        when (viewAction) {
            is ViewAction.BrandChoiceChanged -> {
                _cardInputState.update {
                    val entry = it.entry
                    it.copy(
                        entry = entry.copy(
                            cardBrandChoice = viewAction.cardBrandChoice
                        )
                    )
                }
            }
            ViewAction.BrandChoiceOptionsDismissed -> {
                onBrandChoiceOptionsDismissed(_cardInputState.value.entry.cardBrandChoice.brand)
            }
            ViewAction.BrandChoiceOptionsShown -> {
                onBrandChoiceOptionsShown(_cardInputState.value.entry.cardBrandChoice.brand)
            }
        }
    }

    private fun buildDefaultCardEntry(): CardDetailsEntry {
        val entry = CardDetailsEntry(
            cardBrandChoice = card.getPreferredChoice(cardBrandFilter),
            expMonth = card.expiryMonth,
            expYear = card.expiryYear
        )
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                entry.copy(
                    country = billingDetails?.address?.country,
                    postalCode = billingDetails?.address?.postalCode
                )
            }
            AddressCollectionMode.Never -> {
                entry
            }
            AddressCollectionMode.Full -> {
                entry.copy(
                    line1 = billingDetails?.address?.line1,
                    line2 = billingDetails?.address?.line2,
                    city = billingDetails?.address?.city,
                    state = billingDetails?.address?.state,
                    country = billingDetails?.address?.country,
                    postalCode = billingDetails?.address?.postalCode
                )
            }
        }
    }

    fun dateChanged(text: String) {
        val map = CardDetailsUtil.createExpiryDateFormFieldValues(FormFieldEntry(text))
        _cardInputState.update {
            val entry = it.entry
            it.copy(
                entry = entry.copy(
                    expYear = map[IdentifierSpec.CardExpYear]?.value?.toIntOrNull(),
                    expMonth = map[IdentifierSpec.CardExpMonth]?.value?.toIntOrNull(),
                )
            )
        }
        println("TOLUWANI => ${_cardInputState.value.hasChanged} <=> ${_cardInputState.value.entry}")
    }

    fun validateDate(text: String): TextFieldState {
        return dateConfig.determineState(text)
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
    ): String? {
        return firstOrNull {
            it.first == identifierSpec
        }?.takeIf { it.second.isComplete }
            ?.second?.value
    }

    private fun formattedExpiryDate(): String {
        cardBillingAddressElement.getFormFieldValueFlow()
        val expiryMonth = card.expiryMonth
        val expiryYear = card.expiryYear
        if (expiryMonth == null || expiryYear == null) return ""
        val formattedExpiryMonth = if (expiryMonth < OCTOBER) {
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

    sealed interface ViewAction {
        data object BrandChoiceOptionsShown : ViewAction
        data class BrandChoiceChanged(val cardBrandChoice: CardBrandChoice) : ViewAction
        data object BrandChoiceOptionsDismissed : ViewAction
    }

    companion object {
        fun factory(
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
            addressCollectionMode: AddressCollectionMode,
            cardBrandFilter: CardBrandFilter,
            onBrandChoiceOptionsShown: (CardBrand) -> Unit,
            onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    CardUIViewModel(
                        card = card,
                        billingDetails = billingDetails,
                        addressCollectionMode = addressCollectionMode,
                        cardBrandFilter = cardBrandFilter,
                        onBrandChoiceOptionsDismissed = onBrandChoiceOptionsDismissed,
                        onBrandChoiceOptionsShown = onBrandChoiceOptionsShown
                    )
                }
            }
        }
    }
}

@Immutable
internal data class CardInputState(
    private val card: PaymentMethod.Card,
    private val cardBrandChoice: CardBrandChoice,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode,
    val entry: CardDetailsEntry
) {
    val hasChanged: Boolean
        get() {
            val expChanged = card.expiryMonth != entry.expMonth || card.expiryYear != entry.expYear
            val cardBrandChanged = cardBrandChoice != entry.cardBrandChoice
            val addressChanged = when (addressCollectionMode) {
                AddressCollectionMode.Automatic -> {
                    billingDetails?.address?.postalCode != entry.postalCode ||
                        billingDetails?.address?.country != entry.country
                }
                AddressCollectionMode.Never -> false
                AddressCollectionMode.Full -> {
                    billingDetails?.address?.postalCode != entry.postalCode ||
                        billingDetails?.address?.country != entry.country ||
                        billingDetails?.address?.line1 != entry.line1 ||
                        billingDetails?.address?.line2 != entry.line2 ||
                        billingDetails?.address?.city != entry.city ||
                        billingDetails?.address?.state != entry.state
                }
            }
            return expChanged || cardBrandChanged || addressChanged
        }

    fun addressValid(): Boolean {
        when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                (entry.country.isNullOrBlank() || entry.postalCode.isNullOrBlank()).not()
            }
            AddressCollectionMode.Never -> true
            AddressCollectionMode.Full -> {

            }
        }
    }
}

internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
    val expMonth: Int? = null,
    val expYear: Int? = null,
    val city: String? = null,
    val country: String? = null, // two-character country code
    val line1: String? = null,
    val line2: String? = null,
    val postalCode: String? = null,
    val state: String? = null
)

internal data class CardUpdateParams(
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,
    val cardBrand: CardBrand? = null,
    val billingDetails: PaymentMethod.BillingDetails? = null,
)

internal fun CardDetailsEntry.toUpdateParams(collectAddress: Boolean): CardUpdateParams {
    return CardUpdateParams(
        expiryMonth = expMonth,
        expiryYear = expYear,
        cardBrand = cardBrandChoice.brand,
        billingDetails = when (collectAddress) {
            true -> {
                val address = Address(
                    city = city,
                    country = country,
                    line1 = line1,
                    line2 = line2,
                    postalCode = postalCode,
                    state = state
                )
                PaymentMethod.BillingDetails(address)
            }
            false -> null
        }
    )
}