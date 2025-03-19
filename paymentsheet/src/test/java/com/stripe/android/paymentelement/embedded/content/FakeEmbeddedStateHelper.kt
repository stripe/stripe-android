@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi

internal class FakeEmbeddedStateHelper : EmbeddedStateHelper {
    private val _stateTurbine = Turbine<EmbeddedPaymentElement.State?>()
    val stateTurbine: ReceiveTurbine<EmbeddedPaymentElement.State?> = _stateTurbine

    override var state: EmbeddedPaymentElement.State?
        get() = _stateTurbine.takeItem()
        set(value) {
            _stateTurbine.add(value)
        }

    fun validate() {
        _stateTurbine.ensureAllEventsConsumed()
    }
}
