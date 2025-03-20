package com.stripe.android.customersheet.analytics

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.model.CardBrand

internal interface CustomerSheetEventReporter {

    /**
     * User entered the [CustomerSheet] flow
     */
    fun onInit(
        configuration: CustomerSheet.Configuration,
        integrationType: CustomerSheetIntegration.Type,
    )

    /**
     * [Screen] was presented to user
     */
    fun onScreenPresented(screen: Screen)

    /**
     * [Screen] was hidden to user
     */
    fun onScreenHidden(screen: Screen)

    /**
     * User has selected one of the available payment methods in the payment method form.
     */
    fun onPaymentMethodSelected(code: String)

    /**
     * User attempted to confirm their saved payment method selection and succeeded
     */
    fun onConfirmPaymentMethodSucceeded(
        type: String,
        syncDefaultEnabled: Boolean?,
    )

    /**
     * User attempted to confirm their saved payment method selection and failed
     */
    fun onConfirmPaymentMethodFailed(
        type: String,
        syncDefaultEnabled: Boolean?,
    )

    /**
     * User tapped on edit button
     */
    fun onEditTapped()

    /**
     * User tapped on done button in edit flow
     */
    fun onEditCompleted()

    /**
     * User attempted to remove a saved payment method and succeeded
     */
    fun onRemovePaymentMethodSucceeded()

    /**
     * User attempted to remove a saved payment method and failed
     */
    fun onRemovePaymentMethodFailed()

    /**
     * User attempted to add a saved payment method and succeeded
     */
    fun onAttachPaymentMethodSucceeded(style: AddPaymentMethodStyle)

    /**
     * User attempted to add a saved payment method and canceled
     */
    fun onAttachPaymentMethodCanceled(style: AddPaymentMethodStyle)

    /**
     * User attempted to add a saved payment method and failed
     */
    fun onAttachPaymentMethodFailed(style: AddPaymentMethodStyle)

    /**
     * User attempted to show payment option brands if payment
     * option support card brand choice selection.
     */
    fun onShowPaymentOptionBrands(
        source: CardBrandChoiceEventSource,
        selectedBrand: CardBrand,
    )

    /**
     * User attempted to hide payment option brands if payment
     * option support card brand choice selection.
     */
    fun onHidePaymentOptionBrands(
        source: CardBrandChoiceEventSource,
        selectedBrand: CardBrand?,
    )

    /**
     * User selected a card brand from the card brand choice dropdown.
     */
    fun onBrandChoiceSelected(
        source: CardBrandChoiceEventSource,
        selectedBrand: CardBrand,
    )

    /**
     * User successfully updated the details of a payment method.
     */
    fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand?,
    )

    /**
     * User failed to updated the details of a payment method.
     */
    fun onUpdatePaymentMethodFailed(
        selectedBrand: CardBrand?,
        error: Throwable,
    )

    /**
     * User completed entering their card number for a card
     * payment method.
     */
    fun onCardNumberCompleted()

    fun onDisallowedCardBrandEntered(brand: CardBrand)

    enum class Screen(val value: String) {
        AddPaymentMethod("add_payment_method"),
        SelectPaymentMethod("select_payment_method"),
        EditPaymentMethod("edit_payment_method"),
    }

    enum class AddPaymentMethodStyle(val value: String) {
        SetupIntent("setup_intent"),
        CreateAttach("create_attach")
    }

    enum class CardBrandChoiceEventSource {
        Add, Edit
    }
}
