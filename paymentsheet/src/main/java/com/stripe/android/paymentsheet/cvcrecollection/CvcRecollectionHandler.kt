package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal interface CvcRecollectionHandler {
    fun launch(
        paymentMethod: PaymentMethod,
        launch: (CvcRecollectionData) -> Unit
    )

    fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): Boolean

    fun requiresCVCRecollection(
        stripeIntent: StripeIntent,
        paymentMethod: PaymentMethod,
        optionsParams: PaymentMethodOptionsParams?,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): Boolean
}
