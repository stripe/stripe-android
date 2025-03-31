package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Immutable
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardUpdateParams
import kotlinx.coroutines.flow.StateFlow

internal typealias CardDetailsCallback = (CardUpdateParams?) -> Unit

internal typealias BrandChoiceCallback = (CardBrand) -> Unit

/**
 * Interface for handling UI interactions when editing card details.
 */
internal interface CardEditUIHandler {
    /**
     * The card being edited.
     */
    val card: PaymentMethod.Card

    /**
     * Filter for determining which card brands are available.
     */
    val cardBrandFilter: CardBrandFilter

    /**
     * Icon resource ID for the payment method.
     */
    val paymentMethodIcon: Int

    /**
     * Whether to show the card brand dropdown.
     */
    val showCardBrandDropdown: Boolean

    /**
     * Current state of the card edit UI.
     */
    val state: StateFlow<State>

    /**
     * Callback for when the card brand choice changes.
     */
    val onBrandChoiceChanged: BrandChoiceCallback

    /**
     * Callback for when card details change. It provides the values needed to
     * update the card, if any.
     */
    val onCardDetailsChanged: CardDetailsCallback

    /**
     * Handle a change in the selected card brand. This will be called from the UI when
     * the users selects a card brand.
     *
     * @param cardBrandChoice The newly selected card brand choice
     */
    fun onBrandChoiceChanged(cardBrandChoice: CardBrandChoice)

    /**
     * Represents the current state of the card edit UI.
     *
     * @property selectedCardBrand The currently selected card brand
     */
    @Immutable
    data class State(
        val selectedCardBrand: CardBrandChoice
    )

    /**
     * Factory for creating CardEditUIHandler instances.
     */
    fun interface Factory {
        /**
         * Create a new CardEditUIHandler instance.
         *
         * @param card The card to edit
         * @param cardBrandFilter Filter for available card brands
         * @param showCardBrandDropdown Whether to show the card brand dropdown
         * @param paymentMethodIcon Icon resource for the payment method
         * @param onCardDetailsChanged Callback for card value changes
         */
        fun create(
            card: PaymentMethod.Card,
            cardBrandFilter: CardBrandFilter,
            showCardBrandDropdown: Boolean,
            paymentMethodIcon: Int,
            onCardDetailsChanged: CardDetailsCallback
        ): CardEditUIHandler
    }
}
