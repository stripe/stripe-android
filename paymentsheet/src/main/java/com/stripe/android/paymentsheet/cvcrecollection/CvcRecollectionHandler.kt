package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal interface CvcRecollectionHandler {
    fun launch(
        paymentMethod: PaymentMethod,
        launch: (CvcRecollectionData) -> Unit
    )

    fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent,
    ): Boolean

    fun requiresCVCRecollection(
        stripeIntent: StripeIntent,
        paymentMethod: PaymentMethod,
        optionsParams: PaymentMethodOptionsParams?,
    ): Boolean
}
