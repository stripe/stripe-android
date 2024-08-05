package com.stripe.android.paymentsheet

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.cvcrecollection.CVCRecollectionHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal class FakeCVCRecollectionHandler : CVCRecollectionHandler {
    override fun launch(paymentSelection: PaymentSelection?, launch: (CvcRecollectionData) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent?,
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode?,
        extraRequirements: () -> Boolean
    ): Boolean {
        TODO("Not yet implemented")
    }
}