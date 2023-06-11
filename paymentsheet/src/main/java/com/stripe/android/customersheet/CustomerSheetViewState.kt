package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen

internal sealed class CustomerSheetViewState(
    open val isLiveMode: Boolean = false,
    open val isProcessing: Boolean = false,
    open val isEditing: Boolean = false,
    open val screen: PaymentSheetScreen = PaymentSheetScreen.Loading,
) {
    data class Loading(
        override val isLiveMode: Boolean = false,
    ) : CustomerSheetViewState(
        isLiveMode = isLiveMode,
    )

    data class SelectPaymentMethod(
        val title: String?,
        val savedPaymentMethods: List<PaymentMethod>,
        val paymentSelection: PaymentSelection?,
        val showEditMenu: Boolean,
        override val isProcessing: Boolean,
        override val isEditing: Boolean,
        val isGooglePayEnabled: Boolean,
        val primaryButtonLabel: String?,
        val primaryButtonEnabled: Boolean,
        val errorMessage: String? = null,
    ) : CustomerSheetViewState(
        isProcessing = isProcessing,
        isEditing = isEditing,
        screen = PaymentSheetScreen.SelectSavedPaymentMethods
    )

    data class AddCard(
        val cardNumber: String,
    ) : CustomerSheetViewState(
        isProcessing = false,
        isEditing = false,
        screen = PaymentSheetScreen.AddAnotherPaymentMethod
    )
}
