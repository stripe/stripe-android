package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakeCvcRecollectionHandler : CvcRecollectionHandler {
    var cvcRecollectionEnabled = false
    var requiresCVCRecollection = false

    override fun launch(paymentMethod: PaymentMethod, launch: (CvcRecollectionData) -> Unit) {
        CvcRecollectionData.fromPaymentSelection(paymentMethod.card)?.let(launch)
    }

    override fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent,
        initializationMode: PaymentElementLoader.InitializationMode
    ) = requiresCVCRecollection || cvcRecollectionEnabled

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent,
        paymentMethod: PaymentMethod,
        optionsParams: PaymentMethodOptionsParams?,
        initializationMode: PaymentElementLoader.InitializationMode,
    ) = requiresCVCRecollection
}
