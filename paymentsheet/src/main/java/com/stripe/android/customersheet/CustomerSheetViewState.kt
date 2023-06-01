package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import kotlinx.coroutines.flow.Flow

internal sealed class CustomerSheetViewState(
    open val savedPaymentMethods: List<PaymentMethod>,
    open val isLiveMode: Boolean,
    open val isProcessing: Boolean,
    open val isEditing: Boolean,
    open val screen: PaymentSheetScreen,
) {

    val topBarState: PaymentSheetTopBarState
        get() = PaymentSheetTopBarStateFactory.create(
            screen = screen,
            paymentMethods = savedPaymentMethods,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
        )

    data class Loading(
        override val isLiveMode: Boolean,
    ) : CustomerSheetViewState(
        savedPaymentMethods = emptyList(),
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = PaymentSheetScreen.Loading,
    )

    data class SelectPaymentMethod(
        val title: String?,
        override val savedPaymentMethods: List<PaymentMethod>,
        val paymentSelection: PaymentSelection?,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        override val isEditing: Boolean,
        val isGooglePayEnabled: Boolean,
        val primaryButtonLabel: String?,
        val primaryButtonEnabled: Boolean,
        val errorMessage: String? = null,
    ) : CustomerSheetViewState(
        savedPaymentMethods = savedPaymentMethods,
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
        savedPaymentMethods = emptyList(),
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = PaymentSheetScreen.AddAnotherPaymentMethod
    )
}
