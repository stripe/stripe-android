package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethod

class FakePaymentMethodHolder(
    override val paymentMethod: PaymentMethod?
) : TapToAddPaymentMethodHolder {
    private val _setCalls = Turbine<PaymentMethod?>()
    val setCalls: ReceiveTurbine<PaymentMethod?> = _setCalls

    override fun setPaymentMethod(paymentMethod: PaymentMethod?) {
        _setCalls.add(paymentMethod)
    }

    fun validate() {
        _setCalls.ensureAllEventsConsumed()
    }
}
