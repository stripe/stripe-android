package com.stripe.android.paymentsheet.cvcrecollection

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import com.stripe.android.paymentsheet.state.PaymentElementLoader

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
        initializationMode: PaymentElementLoader.InitializationMode,
    ): Boolean {
        return when (initializationMode) {
            is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                initializationMode.intentConfiguration.requireCvcRecollection &&
                    initializationMode.intentConfiguration.mode is PaymentSheet.IntentConfiguration.Mode.Payment
            }
            is PaymentElementLoader.InitializationMode.PaymentIntent -> stripeIntent.supportsCvcRecollection()
            is PaymentElementLoader.InitializationMode.SetupIntent -> false
        }
    }

    override fun requiresCVCRecollection(
        stripeIntent: StripeIntent,
        paymentMethod: PaymentMethod,
        optionsParams: PaymentMethodOptionsParams?,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): Boolean {
        val hasNotRecollectedCvcAlready = optionsParams == null || !optionsParams.hasAlreadyRecollectedCvc()

        return paymentMethod.isCard() &&
            cvcRecollectionEnabled(stripeIntent, initializationMode) &&
            hasNotRecollectedCvcAlready
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

    private fun PaymentMethodOptionsParams.hasAlreadyRecollectedCvc(): Boolean {
        return when (this) {
            is PaymentMethodOptionsParams.Card -> cvc != null
            else -> false
        }
    }
}
