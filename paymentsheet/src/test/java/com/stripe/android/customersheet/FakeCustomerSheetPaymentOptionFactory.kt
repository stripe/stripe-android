@file:Suppress("DEPRECATION")

package com.stripe.android.customersheet

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakeCustomerSheetPaymentOptionFactory(
    val paymentOption: PaymentOption = PaymentOption(
        drawableResourceId = 0,
        label = "Test payment option",
    ),
) : CustomerSheetPaymentOptionFactory {
    val createCalls = mutableListOf<CreateCall>()

    override fun create(
        selection: PaymentSelection,
        appearance: PaymentSheet.Appearance,
    ): PaymentOption {
        createCalls += CreateCall(
            selection = selection,
            appearance = appearance,
        )
        return paymentOption
    }

    data class CreateCall(
        val selection: PaymentSelection,
        val appearance: PaymentSheet.Appearance,
    )
}
