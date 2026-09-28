package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import javax.inject.Inject
import javax.inject.Singleton

internal interface SheetActivityRegistrar {
    fun registerAndBootstrap(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    )
}

@Singleton
internal class DefaultSheetActivityRegistrar @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val tapToAddHelper: TapToAddHelper,
    private val paymentMethodMetadata: PaymentMethodMetadata,
) : SheetActivityRegistrar {

    private var isBootstrapped = false

    override fun registerAndBootstrap(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
        tapToAddHelper.register(activityResultCaller, lifecycleOwner)
        if (!isBootstrapped) {
            confirmationHandler.bootstrap(paymentMethodMetadata)
            isBootstrapped = true
        }
    }
}
