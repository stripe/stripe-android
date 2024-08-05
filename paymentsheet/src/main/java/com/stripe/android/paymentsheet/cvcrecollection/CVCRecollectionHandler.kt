package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal interface CVCRecollectionHandler {
    fun launch(paymentSelection: PaymentSelection?, launch: (CvcRecollectionData) -> Unit)

    fun requiresCVCRecollection(
        stripeIntent: StripeIntent?,
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode?,
        extraRequirements: () -> Boolean = { true }
    ): Boolean
}
