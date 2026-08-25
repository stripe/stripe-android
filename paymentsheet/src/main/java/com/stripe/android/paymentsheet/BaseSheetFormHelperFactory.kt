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
            cardAccountRangeRepositoryFactory = viewModel.cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = { viewModel.newPaymentSelection },
            selectionUpdater = viewModel::updateSelection,
            linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
            setAsDefaultMatchesSaveForFutureUse = viewModel.customerStateHolder.paymentMethods.value.isEmpty(),
            eventReporter = viewModel.eventReporter,
            savedStateHandle = viewModel.savedStateHandle,
            autocompleteAddressInteractorFactory = viewModel.autocompleteAddressInteractorFactory,
            automaticallyLaunchedCardScanFormDataHelper = createAutomaticallyLaunchedCardScanFormDataHelper(
                shouldCreate = shouldCreateAutomaticallyLaunchedCardScanFormDataHelper,
                paymentMethodMetadata = paymentMethodMetadata,
            ),
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
