package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewPaymentOptionSelection
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class EmbeddedFormHelperFactory @Inject constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val savedStateHandle: SavedStateHandle,
) {
    fun create(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        eventReporter: EventReporter,
        selectionUpdater: (PaymentSelection?) -> Unit,
    ): FormHelper {
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
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            eventReporter = eventReporter,
            savedStateHandle = savedStateHandle,
        )
    }
}
