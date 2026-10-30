package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakeSheetLinkPaymentMethodSelectionHandler(
    var launchResult: Boolean = false,
) : SheetLinkPaymentMethodSelectionHandler {
    val registerCalls = Turbine<RegisterCall>()
    val launchIfEligibleCalls = Turbine<PaymentSelection?>()

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        registerCalls.add(RegisterCall(activityResultCaller, lifecycleOwner))
    }

    override fun launchIfEligible(selection: PaymentSelection?): Boolean {
        launchIfEligibleCalls.add(selection)
        return launchResult
    }

    fun validate() {
        registerCalls.ensureAllEventsConsumed()
        launchIfEligibleCalls.ensureAllEventsConsumed()
    }

    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
    )
}
