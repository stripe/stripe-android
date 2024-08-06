package com.stripe.android.paymentsheet

import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract.Args.DisplayMode

internal object CVCRecollectionFixtures {
    fun contractArgs(
        appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        displayMode: DisplayMode = DisplayMode.PaymentScreen(isLiveMode = true)
    ): CvcRecollectionContract.Args {
        return CvcRecollectionContract.Args(
            lastFour = "0000",
            cardBrand = CardBrand.Visa,
            appearance = appearance,
            displayMode = displayMode
        )
    }
}
