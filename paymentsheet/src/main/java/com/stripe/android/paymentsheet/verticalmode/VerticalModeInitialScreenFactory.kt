package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object VerticalModeInitialScreenFactory {
    fun create(
        viewModel: BaseSheetViewModel,
        paymentMethodMetadata: PaymentMethodMetadata,
        savedPaymentMethods: List<PaymentMethod>,
        savedPaymentMethodMutator: SavedPaymentMethodMutator,
    ): PaymentSheetScreen {
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()

        if (supportedPaymentMethodTypes.size == 1 && savedPaymentMethods.isEmpty()) {
            return PaymentSheetScreen.VerticalModeForm(
                interactor = DefaultVerticalModeFormInteractor.create(
                    selectedPaymentMethodCode = supportedPaymentMethodTypes.first(),
                    viewModel = viewModel,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
                showsWalletHeader = true,
            )
        }
        val interactor = DefaultPaymentMethodVerticalLayoutInteractor.create(
            viewModel = viewModel,
            paymentMethodMetadata = paymentMethodMetadata,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
        )
        return PaymentSheetScreen.VerticalMode(interactor = interactor)
    }
}
