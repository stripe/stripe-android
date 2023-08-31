package com.stripe.android.customersheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal sealed class CustomerSheetViewState(
    open val savedPaymentMethods: List<PaymentMethod>,
    open val isLiveMode: Boolean,
    open val isProcessing: Boolean,
    open val isEditing: Boolean,
    open val stripeIntent: StripeIntent?,
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
        stripeIntent = null,
        screen = PaymentSheetScreen.Loading,
    )

    data class SelectPaymentMethod(
        val title: String?,
        override val savedPaymentMethods: List<PaymentMethod>,
        val paymentSelection: PaymentSelection?,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        override val isEditing: Boolean,
        override val stripeIntent: StripeIntent?,
        val isGooglePayEnabled: Boolean,
        val primaryButtonVisible: Boolean,
        val primaryButtonLabel: String?,
        val errorMessage: String? = null,
        val unconfirmedPaymentMethod: PaymentMethod? = null,
    ) : CustomerSheetViewState(
        savedPaymentMethods = savedPaymentMethods,
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        isEditing = isEditing,
        stripeIntent = stripeIntent,
        screen = PaymentSheetScreen.SelectSavedPaymentMethods,
    ) {
        val primaryButtonEnabled: Boolean
            get() = !isProcessing
    }

    data class AddPaymentMethod(
        val formViewDataMap: Map<PaymentMethodCode, FormViewModel.ViewData>,
        val enabled: Boolean,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        override val stripeIntent: StripeIntent?,
        val errorMessage: String? = null,
        val isFirstPaymentMethod: Boolean,
        val supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
        val selectedPaymentMethod: LpmRepository.SupportedPaymentMethod,
        val formArgumentsMap: Map<PaymentMethodCode, FormArguments>,
        val primaryButtonUiState: com.stripe.android.paymentsheet.ui.PrimaryButton.UIState,
        val primaryButtonState: com.stripe.android.paymentsheet.ui.PrimaryButton.State,
        val mandateText: String?,
    ) : CustomerSheetViewState(
        savedPaymentMethods = emptyList(),
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        isEditing = false,
        stripeIntent = stripeIntent,
        screen = if (isFirstPaymentMethod) {
            PaymentSheetScreen.AddFirstPaymentMethod
        } else {
            PaymentSheetScreen.AddAnotherPaymentMethod
        },
    ) {
        val formViewData: FormViewModel.ViewData
            get() = formViewDataMap[selectedPaymentMethod.code]!!

        val formArguments: FormArguments
            get() = formArgumentsMap[selectedPaymentMethod.code]!!
    }
}
