package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagingPromotionsHelper
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object VerticalModeInitialScreenFactory {
    fun create(
        viewModel: BaseSheetViewModel,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
        paymentMethodMessagingPromotionsHelper: PaymentMethodMessagingPromotionsHelper
    ): List<PaymentSheetScreen> {
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()
        val bankFormInteractor = BankFormInteractor.create(viewModel)

        if (supportedPaymentMethodTypes.size == 1 && customerStateHolder.paymentMethods.value.isEmpty()) {
            val currencySelectorOptions = (viewModel as? PaymentSheetViewModel)
                ?.latestCheckoutSessionResponse
                ?.adaptivePricingInfo
                ?.let { CurrencySelectorOptionsFactory.create(it) }
            return listOf(
                PaymentSheetScreen.VerticalModeForm(
                    interactor = DefaultVerticalModeFormInteractor.create(
                        selectedPaymentMethodCode = supportedPaymentMethodTypes.first(),
                        viewModel = viewModel,
                        paymentMethodMetadata = paymentMethodMetadata,
                        customerStateHolder = customerStateHolder,
                        bankFormInteractor = bankFormInteractor,
                        currencySelectorOptions = currencySelectorOptions,
                        onCurrencySelected = { currencyOption ->
                            (viewModel as? PaymentSheetViewModel)?.updateCurrency(currencyOption.code)
                        },
                        paymentMethodMessagingPromotionsHelper = paymentMethodMessagingPromotionsHelper
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
                paymentMethodMessagingPromotionsHelper = paymentMethodMessagingPromotionsHelper
            )
            val verticalModeScreen = PaymentSheetScreen.VerticalMode(interactor = interactor)
            add(verticalModeScreen)

            (viewModel.selection.value as? PaymentSelection.New?)?.let { newPaymentSelection ->
                val paymentMethodCode = newPaymentSelection.paymentMethodCreateParams.typeCode

                val formHelper = DefaultFormHelper.create(
                    viewModel = viewModel,
                    paymentMethodMetadata = paymentMethodMetadata,
                    paymentMethodMessagingPromotionsHelper = paymentMethodMessagingPromotionsHelper
                )

                if (formHelper.formTypeForCode(paymentMethodCode) == FormHelper.FormType.UserInteractionRequired) {
                    add(
                        PaymentSheetScreen.VerticalModeForm(
                            interactor = DefaultVerticalModeFormInteractor.create(
                                selectedPaymentMethodCode = paymentMethodCode,
                                viewModel = viewModel,
                                paymentMethodMetadata = paymentMethodMetadata,
                                customerStateHolder = customerStateHolder,
                                bankFormInteractor = bankFormInteractor,
                                paymentMethodMessagingPromotionsHelper = paymentMethodMessagingPromotionsHelper
                            ),
                        )
                    )
                }
            }
        }
    }
}
