package com.stripe.android.utils

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.ExternalPaymentMethodInput

internal class FakeExternalPaymentMethodLauncher : ActivityResultLauncher<ExternalPaymentMethodInput>() {
    private val _calls = Turbine<Launch>()
    val calls: ReceiveTurbine<Launch> = _calls

    override fun launch(input: ExternalPaymentMethodInput?, options: ActivityOptionsCompat?) {
        _calls.add(Launch(input))
    }

    override fun unregister() {
        throw NotImplementedError("Not used in testing!")
    }

    override fun getContract(): ActivityResultContract<ExternalPaymentMethodInput, *> {
        throw NotImplementedError("Not used in testing!")
    }

    data class Launch(
        val input: ExternalPaymentMethodInput?,
    )
}
