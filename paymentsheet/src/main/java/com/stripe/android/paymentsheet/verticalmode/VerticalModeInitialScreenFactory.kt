package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.viewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.BaseSheetFormHelperFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.CoroutineScope
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
                    showsWalletHeader = paymentMethodMetadata.availableWallets.any {
                        it != WalletType.Link || paymentMethodMetadata.shouldShowLinkButton
                    },
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

                // This form helper is discarded after this synchronous lookup, so cancel its scope immediately.
                val formTypeScope = viewModel.viewModelScope.childScope(Dispatchers.Main)
                val formType = createFormHelper(viewModel, formTypeScope, paymentMethodMetadata)
                    .formTypeForCode(paymentMethodCode)
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

    private fun createFormHelper(
        viewModel: BaseSheetViewModel,
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): FormHelper {
        return BaseSheetFormHelperFactory(viewModel).create(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            linkInlineHandler = LinkInlineHandler.create(),
            shouldCreateAutomaticallyLaunchedCardScanFormDataHelper = false,
            paymentMethodMessagePromotionsHelper = null,
        )
    }
}
