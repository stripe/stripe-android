package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.elements.Appearance

internal class FakeBacsMandateConfirmationLauncher : BacsMandateConfirmationLauncher {
    private val _calls = Turbine<Call>()
    val calls: ReceiveTurbine<Call> = _calls

    override fun launch(data: BacsMandateData, appearance: Appearance) {
        _calls.add(
            Call(
                data = data,
                appearance = appearance
            )
        )
    }

    data class Call(
        val data: BacsMandateData,
        val appearance: Appearance,
    )
}
