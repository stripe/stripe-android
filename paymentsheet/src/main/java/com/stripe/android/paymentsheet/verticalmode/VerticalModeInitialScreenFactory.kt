package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal object VerticalModeInitialScreenFactory {
    @Suppress("LongMethod")
    fun create(
        viewModel: BaseSheetViewModel,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?
    ): List<PaymentSheetScreen> {
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()
        val formFactory = viewModel.paymentMethodFormFactory
        val dependencies = viewModel.paymentMethodFormFactoryDependencies(paymentMethodMetadata)
        val bankFormInteractor = formFactory.createBankFormInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionUpdater = dependencies.selectionUpdater,
        )

        if (supportedPaymentMethodTypes.size == 1 && customerStateHolder.paymentMethods.value.isEmpty()) {
            paymentMethodMessagePromotionsHelper?.reportPromotionDisplayed(
                supportedPaymentMethodTypes.first(),
                paymentMethodMetadata
            )
            return listOf(
                PaymentSheetScreen.VerticalModeForm(
                    interactor = formFactory.createVerticalModeFormInteractor(
                        selectedPaymentMethodCode = supportedPaymentMethodTypes.first(),
                        paymentMethodMetadata = paymentMethodMetadata,
                        customerHasSavedPaymentMethods = false,
                        dependencies = dependencies,
                        bankFormInteractor = bankFormInteractor,
                        paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                        onMandateOnlyFormReady = null,
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

                val formType = formFactory.createFormHelper(
                    coroutineScope = dependencies.coroutineScope,
                    linkInlineHandler = com.stripe.android.paymentsheet.LinkInlineHandler.create(),
                    paymentMethodMetadata = paymentMethodMetadata,
                    dependencies = dependencies,
                    paymentMethodMessagePromotionsHelper = null,
                    selectedPaymentMethodCode = paymentMethodCode,
                    createAutomaticallyLaunchedCardScanFormDataHelper = false,
                ).formTypeForCode(paymentMethodCode)

                if (formType == FormHelper.FormType.UserInteractionRequired) {
                    add(
                        PaymentSheetScreen.VerticalModeForm(
                            interactor = formFactory.createVerticalModeFormInteractor(
                                selectedPaymentMethodCode = paymentMethodCode,
                                paymentMethodMetadata = paymentMethodMetadata,
                                customerHasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any {
                                    it.type?.code == paymentMethodCode
                                },
                                dependencies = dependencies,
                                bankFormInteractor = bankFormInteractor,
                                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                                onMandateOnlyFormReady = null,
                            ),
                        )
                    )
                }
            }
        }
    }
}
