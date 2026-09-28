package com.stripe.android.common.taptoadd

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import javax.inject.Inject

@TapToAddScope
internal class TapToAddRegistrar @Inject constructor(
    confirmationHandler: ConfirmationHandler,
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
) {
    init {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
    }
}
