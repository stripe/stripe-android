package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.PaymentSheet

internal class FakeBacsMandateConfirmationLauncher : BacsMandateConfirmationLauncher {
    private val _calls = Turbine<Call>()
    val calls: ReceiveTurbine<Call> = _calls

    override fun launch(data: BacsMandateData, appearance: PaymentSheet.Appearance) {
        _calls.add(
            Call(
                data = data,
                appearance = appearance
            )
        )
    }

    data class Call(
        val data: BacsMandateData,
        val appearance: PaymentSheet.Appearance,
    )
}
