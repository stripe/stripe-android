package com.stripe.android.paymentsheet.state

internal val PaymentElementLoader.InitializationMode.shouldDisableWalletsForAutomaticTaxBilling: Boolean
    get() = (this as? PaymentElementLoader.InitializationMode.CheckoutSession)
        ?.checkoutSessionResponse
        ?.shouldDisableWalletsForAutomaticTaxBilling == true
