package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal class FakeCvcRecollectionHandler : CvcRecollectionHandler {
    var cvcRecollectionEnabled = false
    var requiresCVCRecollection = false

    override fun launch(paymentSelection: PaymentSelection?, launch: (CvcRecollectionData) -> Unit) {
        val card = (paymentSelection as? PaymentSelection.Saved)?.paymentMethod?.card
        CvcRecollectionData.fromPaymentSelection(card)?.let(launch)
    }

    override fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent?,
        initializationMode: PaymentSheet.InitializationMode?
    ) = requiresCVCRecollection || cvcRecollectionEnabled

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent?,
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode?,
        extraRequirements: () -> Boolean
    ) = requiresCVCRecollection && extraRequirements()
}
