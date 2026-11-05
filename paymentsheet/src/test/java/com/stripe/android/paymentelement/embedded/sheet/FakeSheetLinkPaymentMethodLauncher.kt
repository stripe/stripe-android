package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.link.LinkConfiguration

internal class FakeSheetLinkPaymentMethodLauncher : SheetLinkPaymentMethodLauncher {
    private val _registerCalls = Turbine<RegisterCall>()
    val registerCalls: ReceiveTurbine<RegisterCall> = _registerCalls

    private val _presentCalls = Turbine<LinkConfiguration>()
    val presentCalls: ReceiveTurbine<LinkConfiguration> = _presentCalls

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        _registerCalls.add(RegisterCall(activityResultCaller, lifecycleOwner))
    }

    override fun present(linkConfiguration: LinkConfiguration) {
        _presentCalls.add(linkConfiguration)
    }

    fun validate() {
        _registerCalls.ensureAllEventsConsumed()
        _presentCalls.ensureAllEventsConsumed()
    }

    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
    )
}
