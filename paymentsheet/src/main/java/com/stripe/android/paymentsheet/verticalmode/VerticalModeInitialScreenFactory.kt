package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.viewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

internal object VerticalModeInitialScreenFactory {
    fun create(
        viewModel: BaseSheetViewModel,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?
    ): List<PaymentSheetScreen> {
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()
        val bankFormInteractor = BankFormInteractor.create(viewModel)

        if (supportedPaymentMethodTypes.size == 1 && customerStateHolder.paymentMethods.value.isEmpty()) {
            paymentMethodMessagePromotionsHelper?.reportPromotionDisplayed(
                supportedPaymentMethodTypes.first(),
                paymentMethodMetadata
            )
            return listOf(
                PaymentSheetScreen.VerticalModeForm(
                    interactor = DefaultVerticalModeFormInteractor.create(
                        selectedPaymentMethodCode = supportedPaymentMethodTypes.first(),
                        viewModel = viewModel,
                        paymentMethodMetadata = paymentMethodMetadata,
                        customerStateHolder = customerStateHolder,
                        bankFormInteractor = bankFormInteractor,
                        paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper
                    ),
                    showsWalletHeader = paymentMethodMetadata.availableWallets.isNotEmpty(),
                )
            )
        }

        return buildList {
            val interactor = DefaultPaymentMethodVerticalLayoutInteractor.create(
                viewModel = viewModel,
                paymentMethodMetadata = paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
                bankFormInteractor = bankFormInteractor,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper
            )
            val verticalModeScreen = PaymentSheetScreen.VerticalMode(interactor = interactor)
            add(verticalModeScreen)

            (viewModel.selection.value as? PaymentSelection.New?)?.let { newPaymentSelection ->
                val paymentMethodCode = newPaymentSelection.paymentMethodCreateParams.typeCode

                // This form helper only answers formTypeForCode synchronously and is then discarded, so give it a
                // scope we cancel immediately rather than leaking its init collector on viewModelScope for the life
                // of the sheet.
                val formTypeScope = viewModel.viewModelScope.childScope(Dispatchers.Main)
                val formType = DefaultFormHelper.create(
                    viewModel = viewModel,
                    coroutineScope = formTypeScope,
                    paymentMethodMetadata = paymentMethodMetadata
                ).formTypeForCode(paymentMethodCode)
                formTypeScope.cancel()

                if (formType == FormHelper.FormType.UserInteractionRequired) {
                    add(
                        PaymentSheetScreen.VerticalModeForm(
                            interactor = DefaultVerticalModeFormInteractor.create(
                                selectedPaymentMethodCode = paymentMethodCode,
                                viewModel = viewModel,
                                paymentMethodMetadata = paymentMethodMetadata,
                                customerStateHolder = customerStateHolder,
                                bankFormInteractor = bankFormInteractor,
                                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper
                            ),
                        )
                    )
                }
            }
        }
    }
}
