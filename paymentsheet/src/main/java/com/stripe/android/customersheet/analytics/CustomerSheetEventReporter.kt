package com.stripe.android.customersheet.analytics

import com.stripe.android.model.PaymentMethod

internal interface CustomerSheetEventReporter {

    /**
     * [Screen] was presented to user
     */
    fun onScreenPresented(screen: Screen)

    /**
     * User attempted to confirm their saved payment method selection and succeeded
     */
    fun onConfirmPaymentMethodSucceeded(type: PaymentMethod.Type)

    /**
     * User attempted to confirm their saved payment method selection and failed
     */
    fun onConfirmPaymentMethodFailed(type: PaymentMethod.Type)

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

    enum class Screen(val value: String) {
        AddPaymentMethod("add_payment_method"),
        SelectPaymentMethod("select_payment_method"),
    }

    enum class AddPaymentMethodStyle(val value: String) {
        SetupIntent("setup_intent"),
        CreateAttach("create_attach")
    }
}
