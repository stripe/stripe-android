package com.stripe.android.paymentsheet.model

import app.cash.turbine.Turbine
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.PaymentSheet

internal class FakePaymentOptionFactory(
    private val result: PaymentOption,
) : PaymentOptionFactory {
    val createCalls = Turbine<CreateCall>()

    override fun create(
        selection: PaymentSelection,
        linkBrand: LinkBrand?,
        appearance: PaymentSheet.Appearance,
    ): PaymentOption {
        createCalls.add(CreateCall(selection, linkBrand, appearance))
        return result
    }

    fun ensureAllEventsConsumed() {
        createCalls.ensureAllEventsConsumed()
    }

    data class CreateCall(
        val selection: PaymentSelection,
        val linkBrand: LinkBrand?,
        val appearance: PaymentSheet.Appearance,
    )
}
