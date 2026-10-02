package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope

internal class PaymentMethodFormFactory(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val isNfcScanningAvailable: IsNfcScanningAvailable,
) {
    data class FormHelperArguments(
        val coroutineScope: CoroutineScope,
        val linkInlineHandler: LinkInlineHandler,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val newPaymentSelectionProvider: (PaymentMethodCode) -> NewPaymentOptionSelection?,
        val selectionUpdater: (PaymentSelection?) -> Unit,
        val eventReporter: EventReporter,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
        val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
        val tapToAddHelper: TapToAddHelper?,
        val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    )

    data class FormDefinitionArguments(
        val coroutineScope: CoroutineScope,
        val linkInlineHandler: LinkInlineHandler,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val newPaymentSelectionProvider: (PaymentMethodCode) -> NewPaymentOptionSelection?,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
        val automaticallyLaunchedCardScanFormDataHelper: AutomaticallyLaunchedCardScanFormDataHelper?,
        val tapToAddHelper: TapToAddHelper?,
        val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper?,
        val autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
    )

    fun createFormHelper(arguments: FormHelperArguments): FormHelper {
        return DefaultFormHelper(
            coroutineScope = arguments.coroutineScope,
            linkInlineHandler = arguments.linkInlineHandler,
            paymentMethodMetadata = arguments.paymentMethodMetadata,
            selectionUpdater = arguments.selectionUpdater,
            eventReporter = arguments.eventReporter,
            savedStateHandle = savedStateHandle,
            formDefinitionFactory = createFormDefinitionFactory(
                FormDefinitionArguments(
                    coroutineScope = arguments.coroutineScope,
                    linkInlineHandler = arguments.linkInlineHandler,
                    paymentMethodMetadata = arguments.paymentMethodMetadata,
                    newPaymentSelectionProvider = arguments.newPaymentSelectionProvider,
                    setAsDefaultMatchesSaveForFutureUse = arguments.setAsDefaultMatchesSaveForFutureUse,
                    automaticallyLaunchedCardScanFormDataHelper =
                        arguments.automaticallyLaunchedCardScanFormDataHelper,
                    tapToAddHelper = arguments.tapToAddHelper,
                    paymentMethodMessagePromotionsHelper = arguments.paymentMethodMessagePromotionsHelper,
                    autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
                )
            ),
        )
    }

    fun createFormDefinitionFactory(arguments: FormDefinitionArguments): FormDefinitionFactory {
        return DefaultFormDefinitionFactory(
            coroutineScope = arguments.coroutineScope,
            linkInlineHandler = arguments.linkInlineHandler,
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = arguments.paymentMethodMetadata,
            newPaymentSelectionProvider = arguments.newPaymentSelectionProvider,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            setAsDefaultMatchesSaveForFutureUse = arguments.setAsDefaultMatchesSaveForFutureUse,
            autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
            isLinkUI = false,
            automaticallyLaunchedCardScanFormDataHelper =
                arguments.automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = arguments.tapToAddHelper,
            paymentMethodMessagePromotionsHelper = arguments.paymentMethodMessagePromotionsHelper,
            isNfcScanningAvailable = isNfcScanningAvailable,
        )
    }
}
