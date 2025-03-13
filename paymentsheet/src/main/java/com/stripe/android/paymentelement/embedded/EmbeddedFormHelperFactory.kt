package com.stripe.android.paymentelement.embedded

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class EmbeddedFormHelperFactory @Inject constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val embeddedSelectionHolder: EmbeddedSelectionHolder,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
) {
    fun create(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        selectionUpdater: (PaymentSelection?) -> Unit
    ): FormHelper {
        return DefaultFormHelper(
            coroutineScope = coroutineScope,
            linkInlineHandler = LinkInlineHandler.create(),
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = {
                when (val currentSelection = embeddedSelectionHolder.selection.value) {
                    is PaymentSelection.ExternalPaymentMethod -> {
                        NewOrExternalPaymentSelection.External(currentSelection)
                    }
                    is PaymentSelection.New -> {
                        NewOrExternalPaymentSelection.New(currentSelection)
                    }
                    else -> null
                }
            },
            selectionUpdater = selectionUpdater,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE, // TODO for embedded
        )
    }
}
