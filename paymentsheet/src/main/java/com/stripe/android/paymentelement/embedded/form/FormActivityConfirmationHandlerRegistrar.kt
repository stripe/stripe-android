package com.stripe.android.paymentelement.embedded.form

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import javax.inject.Inject

internal interface FormActivityConfirmationHandlerRegistrar {
    fun registerAndBootstrap(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        paymentMethodMetadata: PaymentMethodMetadata
    )
}

internal class DefaultFormActivityConfirmationHandlerRegistrar @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
) : FormActivityConfirmationHandlerRegistrar {

    private var isBootstrapped = false

    override fun registerAndBootstrap(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
        paymentMethodMetadata: PaymentMethodMetadata
    ) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
        if (!isBootstrapped) {
            confirmationHandler.bootstrap(paymentMethodMetadata)
            isBootstrapped = true
        }
    }
}
