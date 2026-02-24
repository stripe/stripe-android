package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.FormElement

internal class FakeSavedPaymentMethodConfirmInteractor : SavedPaymentMethodConfirmInteractor {
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
        displayName = "···· 4242".resolvableString,
        paymentMethod = PaymentMethod(
            id = "pm_123",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.Card.code,
            type = PaymentMethod.Type.Card,
            card = PaymentMethod.Card(
                brand = CardBrand.Visa,
                last4 = "4242",
            )
        )
    )
    override val formElement: FormElement? = null

    class Factory : SavedPaymentMethodConfirmInteractor.Factory {
        override fun create(
            initialSelection: PaymentSelection.Saved,
            updateSelection: (PaymentSelection.Saved) -> Unit
        ): SavedPaymentMethodConfirmInteractor {
            return FakeSavedPaymentMethodConfirmInteractor()
        }
    }
}
