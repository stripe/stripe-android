package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.CheckoutSessionPreview

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CurrencySelector internal constructor() {

    @Composable
    fun Content(
        appearance: Checkout.CurrencySelectorContentAppearance = Checkout.CurrencySelectorContentAppearance(),
    ) {
        // TODO: Delegate to CurrencySelectorToggle using controller's checkoutSession and isLoading StateFlows.
    }
}
