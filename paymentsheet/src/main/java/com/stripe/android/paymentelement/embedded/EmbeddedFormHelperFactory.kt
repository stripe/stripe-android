package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
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

internal class EmbeddedFormHelperFactory @Inject constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val selectedPaymentMethodCode: String,
) {
    fun create(
        coroutineScope: CoroutineScope,
        setAsDefaultMatchesSaveForFutureUse: Boolean,
        paymentMethodMetadata: PaymentMethodMetadata,
        eventReporter: EventReporter,
        selectionUpdater: (PaymentSelection?) -> Unit,
    ): FormHelper {
        val automaticallyLaunchedCardScanFormDataHelper = if (selectedPaymentMethodCode.isNotBlank()) {
            val paymentSelection = embeddedSelectionHolder.selection.value as? PaymentSelection.New
            val isLaunchingCardFormWithNoPreviousCardSelection =
                selectedPaymentMethodCode == PaymentMethod.Type.Card.code &&
                    paymentSelection?.paymentMethodCreateParams == null
            AutomaticallyLaunchedCardScanFormDataHelper(
                hasAutomaticallyLaunchedCardScanInitialValue = !isLaunchingCardFormWithNoPreviousCardSelection,
                savedStateHandle = SavedStateHandle(),
                openCardScanAutomaticallyConfig = paymentMethodMetadata.openCardScanAutomatically,
            )
        } else {
            null
        }
        return DefaultFormHelper(
            coroutineScope = coroutineScope,
            linkInlineHandler = LinkInlineHandler.create(),
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
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
        )
    }
}
