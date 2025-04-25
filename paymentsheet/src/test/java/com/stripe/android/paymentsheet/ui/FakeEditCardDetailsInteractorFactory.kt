package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.CoroutineScope

internal class FakeEditCardDetailsInteractorFactory : EditCardDetailsInteractor.Factory {
    var onCardUpdateParamsChanged: CardUpdateParamsCallback? = null
        private set

    override fun create(
        coroutineScope: CoroutineScope,
        isModifiable: Boolean,
        areExpiryDateAndAddressModificationSupported: Boolean,
        cardBrandFilter: CardBrandFilter,
        payload: EditCardPayload,
        addressCollectionMode: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode,
        onBrandChoiceChanged: CardBrandCallback,
        onCardUpdateParamsChanged: CardUpdateParamsCallback
    ): EditCardDetailsInteractor {
        this.onCardUpdateParamsChanged = onCardUpdateParamsChanged
        return FakeEditCardDetailsInteractor(
            payload = payload,
            shouldShowCardBrandDropdown = isModifiable,
            expiryDateEditEnabled = areExpiryDateAndAddressModificationSupported,
        )
    }
}
