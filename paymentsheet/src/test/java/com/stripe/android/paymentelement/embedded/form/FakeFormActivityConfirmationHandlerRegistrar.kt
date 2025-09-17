package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakeFormActivityConfirmationHandlerRegistrar : FormActivityConfirmationHandlerRegistrar {
    val registerAndBootstrapTurbine = Turbine<RegisterCall>()

    override fun registerAndBootstrap(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        paymentMethodMetadata: PaymentMethodMetadata
    ) {
        registerAndBootstrapTurbine.add(
            RegisterCall(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                paymentMethodMetadata = paymentMethodMetadata
            )
        )
    }

    data class RegisterCall(
        val activityResultCaller: ActivityResultCaller,
        val lifecycleOwner: LifecycleOwner,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )
}
