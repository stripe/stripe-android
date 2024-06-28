package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object VerticalModeInitialScreenFactory {
    fun create(viewModel: BaseSheetViewModel): PaymentSheetScreen {
        val savedPaymentMethods = viewModel.paymentMethods.value
        if (viewModel.supportedPaymentMethods.size == 1 && savedPaymentMethods.isEmpty()) {
            return PaymentSheetScreen.Form(
                interactor = DefaultVerticalModeFormInteractor(
                    selectedPaymentMethodCode = viewModel.supportedPaymentMethods.first().code,
                    viewModel = viewModel,
                ),
                showsWalletHeader = true,
            )
        }
        return PaymentSheetScreen.VerticalMode(DefaultPaymentMethodVerticalLayoutInteractor(viewModel))
    }
}
