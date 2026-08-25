package com.stripe.android.paymentsheet

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import kotlinx.coroutines.CoroutineScope

internal class BaseSheetFormHelperFactory(
    private val viewModel: BaseSheetViewModel,
) {
    fun create(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkInlineHandler: LinkInlineHandler,
        shouldCreateAutomaticallyLaunchedCardScanFormDataHelper: Boolean,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
    ): FormHelper {
        return DefaultFormHelper(
            coroutineScope = coroutineScope,
            linkInlineHandler = linkInlineHandler,
            paymentMethodMetadata = paymentMethodMetadata,
            selectionUpdater = viewModel::updateSelection,
            eventReporter = viewModel.eventReporter,
            savedStateHandle = viewModel.savedStateHandle,
            formDefinitionFactory = createFormDefinitionFactory(
                coroutineScope = coroutineScope,
                paymentMethodMetadata = paymentMethodMetadata,
                linkInlineHandler = linkInlineHandler,
                automaticallyLaunchedCardScanFormDataHelper = createAutomaticallyLaunchedCardScanFormDataHelper(
                    shouldCreate = shouldCreateAutomaticallyLaunchedCardScanFormDataHelper,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            ),
        )
    }

    private fun createFormDefinitionFactory(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkInlineHandler: LinkInlineHandler,
        automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
    ): FormDefinitionFactory {
        return DefaultFormDefinitionFactory(
            coroutineScope = coroutineScope,
            linkInlineHandler = linkInlineHandler,
            cardAccountRangeRepositoryFactory = viewModel.cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = { viewModel.newPaymentSelection },
            linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
            setAsDefaultMatchesSaveForFutureUse = viewModel.customerStateHolder.paymentMethods.value.isEmpty(),
            autocompleteAddressInteractorFactory = viewModel.autocompleteAddressInteractorFactory,
            isLinkUI = false,
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = viewModel.tapToAddHelper,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            isNfcScanningAvailable = viewModel.isNfcScanningAvailable,
        )
    }

    private fun createAutomaticallyLaunchedCardScanFormDataHelper(
        shouldCreate: Boolean,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): AutomaticallyLaunchedCardScanFormDataHelper? {
        if (!shouldCreate) {
            return null
        }

        val hasSeenAutomaticCardScanLaunch =
            viewModel.newPaymentSelection?.paymentSelection is PaymentSelection.New.Card &&
                viewModel.newPaymentSelection?.getPaymentMethodCreateParams() != null

        return AutomaticallyLaunchedCardScanFormDataHelper(
            openCardScanAutomaticallyConfig = paymentMethodMetadata.openCardScanAutomatically,
            savedStateHandle = viewModel.savedStateHandle,
            hasAutomaticallyLaunchedCardScanInitialValue = hasSeenAutomaticCardScanLaunch,
        )
    }
}
