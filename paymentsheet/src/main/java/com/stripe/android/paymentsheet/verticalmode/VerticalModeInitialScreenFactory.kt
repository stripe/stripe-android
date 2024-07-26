package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object VerticalModeInitialScreenFactory {
    fun create(
        viewModel: BaseSheetViewModel,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
        savedPaymentMethodMutator: SavedPaymentMethodMutator,
    ): PaymentSheetScreen {
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()

        if (supportedPaymentMethodTypes.size == 1 && customerStateHolder.paymentMethods.value.isEmpty()) {
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
            customerStateHolder = customerStateHolder,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
        )
        return PaymentSheetScreen.VerticalMode(interactor = interactor)
    }
}
