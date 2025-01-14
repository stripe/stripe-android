package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.viewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object VerticalModeInitialScreenFactory {
    fun create(
        viewModel: BaseSheetViewModel,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
    ): List<PaymentSheetScreen> {
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()
        val bankFormInteractor = BankFormInteractor.create(viewModel)

        if (supportedPaymentMethodTypes.size == 1 && customerStateHolder.paymentMethods.value.isEmpty()) {
            return listOf(
                PaymentSheetScreen.VerticalModeForm(
                    interactor = DefaultVerticalModeFormInteractor.create(
                        selectedPaymentMethodCode = supportedPaymentMethodTypes.first(),
                        viewModel = viewModel,
                        paymentMethodMetadata = paymentMethodMetadata,
                        customerStateHolder = customerStateHolder,
                        bankFormInteractor = bankFormInteractor,
                    ),
                    showsWalletHeader = true,
                )
            )
        }

        return buildList {
            val interactor = DefaultPaymentMethodVerticalLayoutInteractor.create(
                viewModel = viewModel,
                paymentMethodMetadata = paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
                bankFormInteractor = bankFormInteractor,
            )
            val verticalModeScreen = PaymentSheetScreen.VerticalMode(interactor = interactor)
            add(verticalModeScreen)

            (viewModel.selection.value as? PaymentSelection.New?)?.let { newPaymentSelection ->
                val paymentMethodCode = newPaymentSelection.paymentMethodCreateParams.typeCode

                val linkInlineHandler = LinkInlineHandler.create(viewModel, viewModel.viewModelScope)
                val formHelper = DefaultFormHelper.create(viewModel, linkInlineHandler, paymentMethodMetadata)

                if (formHelper.requiresFormScreen(paymentMethodCode)) {
                    add(
                        PaymentSheetScreen.VerticalModeForm(
                            interactor = DefaultVerticalModeFormInteractor.create(
                                selectedPaymentMethodCode = paymentMethodCode,
                                viewModel = viewModel,
                                paymentMethodMetadata = paymentMethodMetadata,
                                customerStateHolder = customerStateHolder,
                                bankFormInteractor = bankFormInteractor,
                            ),
                        )
                    )
                }
            }
        }
    }
}
