package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.CvcRecollectionCallbackHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal class CVCRecollectionHandlerImpl : CVCRecollectionHandler {

    override fun launch(paymentSelection: PaymentSelection?, launch: (CvcRecollectionData) -> Unit) {
        val card = (paymentSelection as? PaymentSelection.Saved)?.paymentMethod?.card
        CvcRecollectionData.fromPaymentSelection(card)?.let(launch)
            ?: throw IllegalStateException("unable to create CvcRecollectionData")
    }

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent?,
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode?,
        extraRequirements: () -> Boolean
    ): Boolean {
        return paymentSelection is PaymentSelection.Saved
            && paymentSelection.paymentMethod.type == PaymentMethod.Type.Card && (
            (stripeIntent as? PaymentIntent)?.requireCvcRecollection == true ||
                (
                    CvcRecollectionCallbackHandler.isCvcRecollectionEnabledForDeferredIntent()
                        &&
                        initializationMode is PaymentSheet.InitializationMode.DeferredIntent
                    )
            )
    }

}