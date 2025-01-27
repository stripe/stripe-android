package com.stripe.android.paymentelement.embedded

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope

internal object EmbeddedFormHelperFactory {
    fun create(
        coroutineScope: CoroutineScope,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkConfigurationCoordinator: LinkConfigurationCoordinator,
        currentSelection: PaymentSelection?,
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        selectionUpdater: (PaymentSelection?) -> Unit
    ): FormHelper {
        return DefaultFormHelper(
            coroutineScope = coroutineScope,
            linkInlineHandler = LinkInlineHandler.create(),
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata,
            newPaymentSelectionProvider = {
                when (currentSelection) {
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
        )
    }
}
