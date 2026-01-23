package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.DEFAULT_ACCOUNT_RANGE_SERVICE_FACTORY
import com.stripe.android.cards.FUNDING_ACCOUNT_RANGE_SERVICE_FACTORY
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewPaymentOptionSelection
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Named

internal class EmbeddedFormHelperFactory @Inject constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    @Named(DEFAULT_ACCOUNT_RANGE_SERVICE_FACTORY) private val cardAccountRangeServiceFactory:
    CardAccountRangeService.Factory,
    @Named(FUNDING_ACCOUNT_RANGE_SERVICE_FACTORY) private val fundingCardAccountRangeServiceFactory:
    CardAccountRangeService.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val selectedPaymentMethodCode: String,
    private val tapToAddCollectionHandler: TapToAddCollectionHandler,
) {
    fun create(
        coroutineScope: CoroutineScope,
        setAsDefaultMatchesSaveForFutureUse: Boolean,
        paymentMethodMetadata: PaymentMethodMetadata,
        eventReporter: EventReporter,
        onError: (ResolvableString?) -> Unit = {},
        updateEnabled: (Boolean) -> Unit = {},
        selectionUpdater: (PaymentSelection?) -> Unit,
    ): FormHelper {
        val automaticallyLaunchedCardScanFormDataHelper = if (selectedPaymentMethodCode.isNotBlank()) {
            val paymentSelection = embeddedSelectionHolder.selection.value as? PaymentSelection.New
            val isLaunchingEmptyCardForm =
                selectedPaymentMethodCode == PaymentMethod.Type.Card.code &&
                    paymentSelection?.paymentMethodCreateParams == null
            AutomaticallyLaunchedCardScanFormDataHelper(
                hasAutomaticallyLaunchedCardScanInitialValue = !isLaunchingEmptyCardForm,
                savedStateHandle = savedStateHandle,
                openCardScanAutomaticallyConfig = paymentMethodMetadata.openCardScanAutomatically,
            )
        } else {
            null
        }
        val tapToAddHelper = TapToAddHelper.create(
            coroutineScope = coroutineScope,
            tapToAddCollectionHandler = tapToAddCollectionHandler,
            paymentMethodMetadata = paymentMethodMetadata,
            onCollectingUpdated = { processing ->
                updateEnabled(processing)
            },
            onError = { error ->
                onError(error)
            },
        )
        return DefaultFormHelper(
            coroutineScope = coroutineScope,
            linkInlineHandler = LinkInlineHandler.create(),
            cardAccountRangeServiceFactory = cardAccountRangeServiceFactory,
            fundingCardAccountRangeServiceFactory = fundingCardAccountRangeServiceFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = {
                when (val currentSelection = embeddedSelectionHolder.selection.value) {
                    is PaymentSelection.ExternalPaymentMethod -> {
                        NewPaymentOptionSelection.External(currentSelection)
                    }
                    is PaymentSelection.CustomPaymentMethod -> {
                        NewPaymentOptionSelection.Custom(currentSelection)
                    }
                    is PaymentSelection.New -> {
                        NewPaymentOptionSelection.New(currentSelection)
                    }
                    else -> null
                }
            },
            selectionUpdater = selectionUpdater,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            setAsDefaultMatchesSaveForFutureUse = setAsDefaultMatchesSaveForFutureUse,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
            autocompleteAddressInteractorFactory = null,
            automaticallyLaunchedCardScanFormDataHelper = automaticallyLaunchedCardScanFormDataHelper,
            tapToAddHelper = tapToAddHelper,
        )
    }
}
