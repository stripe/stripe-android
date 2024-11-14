package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal interface CvcRecollectionHandler {
    fun launch(paymentSelection: PaymentSelection?, launch: (CvcRecollectionData) -> Unit)

    fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent?,
        initializationMode: PaymentElementLoader.InitializationMode?,
    ): Boolean

    fun requiresCVCRecollection(
        stripeIntent: StripeIntent?,
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentElementLoader.InitializationMode?,
        extraRequirements: () -> Boolean = { true }
    ): Boolean
}
