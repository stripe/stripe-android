package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakeTagValueProducer(
    private val tag: String,
    private val value: ByteArray,
) : TagValueProducer {
    val produceCalls = Turbine<PaymentMethodMetadata>()

    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        produceCalls.add(paymentMethodMetadata)
        return TagValueProducer.Result(
            tag = tag,
            value = value,
        )
    }

    fun ensureAllEventsConsumed() {
        produceCalls.ensureAllEventsConsumed()
    }
}
