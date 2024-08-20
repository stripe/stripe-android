package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal class FakeCvcRecollectionHandler: CvcRecollectionHandler {
    var cvcRecollectionEnabled = false
    var requiresCVCRecollection = false

    override fun launch(paymentSelection: PaymentSelection?, launch: (CvcRecollectionData) -> Unit) = Unit

    override fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent?,
        initializationMode: PaymentSheet.InitializationMode?
    ) = cvcRecollectionEnabled

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent?,
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode?,
        extraRequirements: () -> Boolean
    ) = requiresCVCRecollection
}