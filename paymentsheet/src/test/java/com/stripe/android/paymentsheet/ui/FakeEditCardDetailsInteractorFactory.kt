package com.stripe.android.paymentsheet.ui

import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import kotlinx.coroutines.CoroutineScope

internal class FakeEditCardDetailsInteractorFactory : EditCardDetailsInteractor.Factory {
    var onCardUpdateParamsChanged: CardUpdateParamsCallback? = null
        private set

    override fun create(
        coroutineScope: CoroutineScope,
        cardEditConfiguration: CardEditConfiguration?,
        requiresModification: Boolean,
        payload: EditCardPayload,
        billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        onBrandChoiceChanged: CardBrandCallback,
        onCardUpdateParamsChanged: CardUpdateParamsCallback
    ): EditCardDetailsInteractor {
        this.onCardUpdateParamsChanged = onCardUpdateParamsChanged
        return FakeEditCardDetailsInteractor(
            payload = payload,
            shouldShowCardBrandDropdown = cardEditConfiguration?.isCbcModifiable ?: false,
            expiryDateEditEnabled = cardEditConfiguration?.areExpiryDateAndAddressModificationSupported ?: false,
        )
    }
}
