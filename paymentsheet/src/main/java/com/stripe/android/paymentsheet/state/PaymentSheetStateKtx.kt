package com.stripe.android.paymentsheet.state

import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.toPaymentSelection

internal fun PaymentSheetState.Full.removePaymentMethod(
    paymentMethodId: String
): PaymentSheetState.Full {
    val currentSelection = (selection as? PaymentSelection.Saved)?.paymentMethod?.id
    val didRemoveSelectedItem = currentSelection == paymentMethodId

    val updatedPaymentMethods = customerPaymentMethods.filter { it.id != paymentMethodId }
    val hasNoBankAccounts = updatedPaymentMethods.all { it.type != USBankAccount }

    val newSelection = if (didRemoveSelectedItem) {
        PaymentOptionsStateFactory.create(
            paymentMethods = updatedPaymentMethods,
            showGooglePay = true,
            showLink = true,
            initialSelection = savedSelection,
        ).selectedItem?.toPaymentSelection()
    } else {
        selection
    }

    val newPrimaryButtonUiState = primaryButtonUiState?.copy(
        visible = if (hasNoBankAccounts) false else primaryButtonUiState.visible,
    )

    return copy(
        customerPaymentMethods = updatedPaymentMethods,
        selection = newSelection,
        notesText = if (hasNoBankAccounts) null else notesText,
        primaryButtonUiState = newPrimaryButtonUiState,
    )
}

internal fun PaymentSheetState.Full.loggedInToLink(): PaymentSheetState.Full {
    return copy(
        linkState = linkState?.copy(
            loginState = LinkState.LoginState.LoggedIn,
        )
    )
}

internal fun PaymentSheetState.Full.loggedOutOfLink(): PaymentSheetState.Full {
    return copy(
        linkState = linkState?.copy(
            loginState = LinkState.LoginState.LoggedOut,
        )
    )
}
