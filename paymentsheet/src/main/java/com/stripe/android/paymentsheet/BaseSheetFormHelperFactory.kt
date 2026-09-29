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
    private val formFactory = PaymentMethodFormFactory(
        linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
        cardAccountRangeRepositoryFactory = viewModel.cardAccountRangeRepositoryFactory,
        savedStateHandle = viewModel.savedStateHandle,
        isNfcScanningAvailable = viewModel.isNfcScanningAvailable,
    )

    fun create(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkInlineHandler: LinkInlineHandler,
        shouldCreateAutomaticallyLaunchedCardScanFormDataHelper: Boolean,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
    ): FormHelper {
        return formFactory.createFormHelper(
            PaymentMethodFormFactory.FormHelperArguments(
                coroutineScope = coroutineScope,
                linkInlineHandler = linkInlineHandler,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = { viewModel.newPaymentSelection },
                selectionUpdater = viewModel::updateSelection,
                eventReporter = viewModel.eventReporter,
                setAsDefaultMatchesSaveForFutureUse = viewModel.customerStateHolder.paymentMethods.value.isEmpty(),
                automaticallyLaunchedCardScanFormDataHelper = createAutomaticallyLaunchedCardScanFormDataHelper(
                    shouldCreate = shouldCreateAutomaticallyLaunchedCardScanFormDataHelper,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
                tapToAddHelper = viewModel.tapToAddHelper,
                paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
                autocompleteAddressInteractorFactory = viewModel.autocompleteAddressInteractorFactory,
            )
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
