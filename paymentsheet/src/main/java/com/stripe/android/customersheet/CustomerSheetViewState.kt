package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import kotlinx.coroutines.flow.Flow

internal sealed class CustomerSheetViewState(
    open val showEditMenu: Boolean,
    open val isLiveMode: Boolean,
    open val isProcessing: Boolean,
    open val isEditing: Boolean,
    open val screen: PaymentSheetScreen,
) {
    data class Loading(
        override val isLiveMode: Boolean,
    ) : CustomerSheetViewState(
        showEditMenu = false,
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = PaymentSheetScreen.Loading,
    )

    data class SelectPaymentMethod(
        val title: String?,
        val savedPaymentMethods: List<PaymentMethod>,
        val paymentSelection: PaymentSelection?,
        override val showEditMenu: Boolean,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        override val isEditing: Boolean,
        val isGooglePayEnabled: Boolean,
        val primaryButtonLabel: String?,
        val primaryButtonEnabled: Boolean,
        val errorMessage: String? = null,
    ) : CustomerSheetViewState(
        showEditMenu = showEditMenu,
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        isEditing = isEditing,
        screen = PaymentSheetScreen.SelectSavedPaymentMethods,
    )

    data class AddPaymentMethod(
        val formViewDataFlow: Flow<FormViewModel.ViewData>,
        val enabled: Boolean,
        override val isLiveMode: Boolean,
    ) : CustomerSheetViewState(
        showEditMenu = false,
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = PaymentSheetScreen.AddAnotherPaymentMethod
    )
}
