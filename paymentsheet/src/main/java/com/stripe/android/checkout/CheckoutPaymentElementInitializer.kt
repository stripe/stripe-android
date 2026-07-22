package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import javax.inject.Inject

internal class CheckoutPaymentElementInitializer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
) {
    fun initialize() {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
    }
}
