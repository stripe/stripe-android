package com.stripe.android.customersheet.analytics

import com.stripe.android.model.CardBrand

internal interface CustomerSheetEventReporter {

    /**
     * [Screen] was presented to user
     */
    fun onScreenPresented(screen: Screen)

    /**
     * [Screen] was hidden to user
     */
    fun onScreenHidden(screen: Screen)

    /**
     * User attempted to confirm their saved payment method selection and succeeded
     */
    fun onConfirmPaymentMethodSucceeded(type: String)

    /**
     * User attempted to confirm their saved payment method selection and failed
     */
    fun onConfirmPaymentMethodFailed(type: String)

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
     * User successfully updated the card brand choice selection for
     * a payment method.
     */
    fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand,
    )

    /**
     * User failed to updated the card brand choice selection for
     * a payment method.
     */
    fun onUpdatePaymentMethodFailed(
        selectedBrand: CardBrand,
        error: Throwable,
    )

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
