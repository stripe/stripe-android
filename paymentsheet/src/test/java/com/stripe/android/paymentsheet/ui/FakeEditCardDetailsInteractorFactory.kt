package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.CoroutineScope

internal class FakeEditCardDetailsInteractorFactory : EditCardDetailsInteractor.Factory {
    var onCardUpdateParamsChanged: CardUpdateParamsCallback? = null
        private set

    override fun create(
        coroutineScope: CoroutineScope,
        cardEditConfiguration: CardEditConfiguration?,
        requiresModification: Boolean,
        payload: EditCardPayload,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
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
