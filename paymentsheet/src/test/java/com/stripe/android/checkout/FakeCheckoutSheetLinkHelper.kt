package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection

internal class FakeCheckoutSheetLinkHelper(
    var launchLinkIfEligibleResult: Boolean = false,
) : CheckoutSheetLinkHelper {
    val registerCalls = Turbine<RegisterCall>()
    val unregisterCalls = Turbine<Unit>()
    val launchLinkIfEligibleCalls = Turbine<LaunchLinkIfEligibleCall>()

    override fun register(
        activityResultCaller: ActivityResultCaller,
        launchPaymentOptions: CheckoutSheetLinkHelper.LaunchPaymentOptions,
    ) {
        registerCalls.add(RegisterCall(activityResultCaller, launchPaymentOptions))
    }

    override fun unregister() {
        unregisterCalls.add(Unit)
    }

    override fun launchLinkIfEligible(
        paymentMethodMetadata: PaymentMethodMetadata,
        selection: PaymentSelection?,
    ): Boolean {
        launchLinkIfEligibleCalls.add(LaunchLinkIfEligibleCall(paymentMethodMetadata, selection))
        return launchLinkIfEligibleResult
    }

    fun ensureAllEventsConsumed() {
        registerCalls.ensureAllEventsConsumed()
        unregisterCalls.ensureAllEventsConsumed()
        launchLinkIfEligibleCalls.ensureAllEventsConsumed()
    }

    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val launchPaymentOptions: CheckoutSheetLinkHelper.LaunchPaymentOptions,
    )

    data class LaunchLinkIfEligibleCall(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val selection: PaymentSelection?,
    )
}
