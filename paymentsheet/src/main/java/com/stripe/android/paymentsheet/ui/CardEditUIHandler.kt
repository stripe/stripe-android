package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.TextFieldState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface CardEditUIHandler {
    val card: PaymentMethod.Card
    val billingDetails: PaymentMethod.BillingDetails?
    val addressCollectionMode: AddressCollectionMode
    val cardBrandFilter: CardBrandFilter
    val onBrandChoiceOptionsShown: (CardBrand) -> Unit
    val onBrandChoiceOptionsDismissed: (CardBrand) -> Unit
    val collectAddress: Boolean
    val addressSectionElement: SectionElement
    val expDate: String
    val hiddenAddressElements: Set<IdentifierSpec>
    val state: StateFlow<State>
    val onCardValuesChanged: (CardUpdateParams?) -> Unit

    fun dateChanged(text: String)
    fun dateValidator(text: String): TextFieldState
    fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice)
    fun onBrandChoiceOptionsDismissed()
    fun onBrandChoiceOptionsShown()

    data class State(
        val card: PaymentMethod.Card,
        val expDate: String,
        val address: Address?,
        val selectedCardBrand: CardBrandChoice
    ) {
        data class Address(
            val addressElement: SectionElement,
            val hiddenAddressFields: Set<IdentifierSpec>,
        )
    }

    fun interface Factory {
        fun create(
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
            addressCollectionMode: AddressCollectionMode,
            cardBrandFilter: CardBrandFilter,
            onBrandChoiceOptionsShown: (CardBrand) -> Unit,
            onBrandChoiceOptionsDismissed: (CardBrand) -> Unit,
            onCardValuesChanged: (CardUpdateParams?) -> Unit
        ): CardEditUIHandler
    }
}