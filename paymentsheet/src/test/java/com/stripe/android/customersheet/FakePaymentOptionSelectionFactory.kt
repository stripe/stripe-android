package com.stripe.android.customersheet

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakePaymentOptionSelectionFactory(
    val result: PaymentOptionSelection? = null,
) : PaymentOptionSelectionFactory {
    val createCalls = Turbine<CreateCall>()

    override fun create(
        selection: PaymentSelection?,
        canUseGooglePay: Boolean,
        appearance: PaymentSheet.Appearance,
    ): PaymentOptionSelection? {
        createCalls.add(CreateCall(selection, canUseGooglePay, appearance))
        return result
    }

    fun ensureAllEventsConsumed() {
        createCalls.ensureAllEventsConsumed()
    }

    data class CreateCall(
        val selection: PaymentSelection?,
        val canUseGooglePay: Boolean,
        val appearance: PaymentSheet.Appearance,
    )
}
