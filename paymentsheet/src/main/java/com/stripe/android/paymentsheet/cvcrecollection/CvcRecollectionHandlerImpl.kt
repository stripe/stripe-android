package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData

internal class CvcRecollectionHandlerImpl : CvcRecollectionHandler {

    override fun launch(paymentSelection: PaymentSelection?, launch: (CvcRecollectionData) -> Unit) {
        val card = (paymentSelection as? PaymentSelection.Saved)?.paymentMethod?.card
        CvcRecollectionData.fromPaymentSelection(card)?.let(launch)
            ?: throw IllegalStateException("unable to create CvcRecollectionData")
    }

    override fun cvcRecollectionEnabled(
        stripeIntent: StripeIntent?,
        initializationMode: PaymentSheet.InitializationMode?
    ): Boolean {
        return deferredIntentRequiresCVCRecollection(initializationMode) ||
            paymentIntentRequiresCVCRecollection(stripeIntent, initializationMode)
    }

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent?,
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode?,
        extraRequirements: () -> Boolean
    ): Boolean {
        return paymentSelectionIsSavedCard(paymentSelection) &&
            cvcRecollectionEnabled(stripeIntent, initializationMode) && extraRequirements()
    }

    private fun deferredIntentRequiresCVCRecollection(initializationMode: PaymentSheet.InitializationMode?): Boolean {
        return (initializationMode as? PaymentSheet.InitializationMode.DeferredIntent)
            ?.intentConfiguration?.requireCvcRecollection == true &&
            initializationMode.intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
    }

    private fun paymentIntentRequiresCVCRecollection(
        stripeIntent: StripeIntent?,
        initializationMode: PaymentSheet.InitializationMode?
    ): Boolean {
        return (stripeIntent as? PaymentIntent)?.requireCvcRecollection == true &&
            initializationMode is PaymentSheet.InitializationMode.PaymentIntent
    }

    private fun paymentSelectionIsSavedCard(paymentSelection: PaymentSelection?): Boolean {
        return paymentSelection is PaymentSelection.Saved &&
            paymentSelection.paymentMethod.type == PaymentMethod.Type.Card
    }
}
