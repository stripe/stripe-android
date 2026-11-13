package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import kotlinx.coroutines.CoroutineScope

internal class FakeEditCardDetailsInteractorFactory : EditCardDetailsInteractor.Factory {
    var billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration? = null
        private set

    var onCardUpdateParamsChanged: CardUpdateParamsCallback? = null
        private set

    var autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory? = null
        private set

    var requiresBillingAddressForAutomaticTax: Boolean? = null
        private set

    override fun create(
        coroutineScope: CoroutineScope,
        cardEditConfiguration: CardEditConfiguration?,
        requiresModification: Boolean,
        payload: EditCardPayload,
        billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        onBrandChoiceChanged: CardBrandCallback,
        onCardUpdateParamsChanged: CardUpdateParamsCallback,
        autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        requiresBillingAddressForAutomaticTax: Boolean,
    ): EditCardDetailsInteractor {
        this.onCardUpdateParamsChanged = onCardUpdateParamsChanged
        this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        this.autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory
        this.requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax
        return FakeEditCardDetailsInteractor(
            payload = payload,
            shouldShowCardBrandDropdown = cardEditConfiguration?.isCbcModifiable ?: false,
            expiryDateEditEnabled = cardEditConfiguration?.areExpiryDateAndAddressModificationSupported ?: false,
        )
    }
}
