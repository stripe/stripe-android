package com.stripe.android.paymentelement.embedded.content

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper

internal class FakeEmbeddedResultCallbackHelper(
    val stateHelper: FakeEmbeddedStateHelper
) : EmbeddedResultCallbackHelper {
    private val _callbackTurbine = Turbine<EmbeddedPaymentElement.Result>()
    val callbackTurbine: ReceiveTurbine<EmbeddedPaymentElement.Result> = _callbackTurbine

    override fun setResult(result: EmbeddedPaymentElement.Result) {
        _callbackTurbine.add(result)
        if (result is EmbeddedPaymentElement.Result.Completed) {
            stateHelper.state = null
        }
    }

    fun validate() {
        stateHelper.validate()
        _callbackTurbine.ensureAllEventsConsumed()
    }
}
