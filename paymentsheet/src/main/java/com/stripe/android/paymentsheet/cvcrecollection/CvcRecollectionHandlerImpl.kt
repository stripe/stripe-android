package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal class CvcRecollectionHandlerImpl : CvcRecollectionHandler {

    override fun launch(
        paymentMethod: PaymentMethod,
        launch: (CvcRecollectionData) -> Unit
    ) {
        CvcRecollectionData.fromPaymentSelection(paymentMethod.card)?.let(launch)
            ?: throw IllegalStateException("unable to create CvcRecollectionData")
    }

    override fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent,
    ): Boolean {
        return when (stripeIntent) {
            is PaymentIntent -> stripeIntent.supportsCvcRecollection()
            is SetupIntent -> false
        }
    }

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent,
        paymentMethod: PaymentMethod,
        optionsParams: PaymentMethodOptionsParams?,
    ): Boolean {
        return paymentMethod.isCard() && cvcRecollectionEnabled(stripeIntent)
    }

    private fun PaymentMethod.isCard(): Boolean {
        return type == PaymentMethod.Type.Card
    }

    private fun StripeIntent.supportsCvcRecollection(): Boolean {
        return when (this) {
            is PaymentIntent -> requireCvcRecollection
            is SetupIntent -> false
        }
    }
}
