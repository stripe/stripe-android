package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.CoroutineScope

internal class FakeEditCardDetailsInteractorFactory : EditCardDetailsInteractor.Factory {
    var onCardUpdateParamsChanged: CardUpdateParamsCallback? = null
        private set

    override fun create(
        coroutineScope: CoroutineScope,
        isCbcModifiable: Boolean,
        requiresModification: Boolean,
        areExpiryDateAndAddressModificationSupported: Boolean,
        cardBrandFilter: CardBrandFilter,
        payload: EditCardPayload,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        onBrandChoiceChanged: CardBrandCallback,
        onCardUpdateParamsChanged: CardUpdateParamsCallback
    ): EditCardDetailsInteractor {
        this.onCardUpdateParamsChanged = onCardUpdateParamsChanged
        return FakeEditCardDetailsInteractor(
            payload = payload,
            shouldShowCardBrandDropdown = isCbcModifiable,
            expiryDateEditEnabled = areExpiryDateAndAddressModificationSupported,
        )
    }
}
