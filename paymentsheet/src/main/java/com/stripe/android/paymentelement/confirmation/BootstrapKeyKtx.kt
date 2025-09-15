package com.stripe.android.paymentelement.confirmation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

internal fun ConfirmationHandler.bootstrapHelper(
    passiveCaptchaParams: PassiveCaptchaParams,
    lifecycleOwner: LifecycleOwner
) {
    bootstrap(mapOf(BootstrapKey.PassiveCaptcha to passiveCaptchaParams), lifecycleOwner)
}

internal fun ConfirmationHandler.bootstrapHelper(
    passiveCaptchaParamsFlow: Flow<PassiveCaptchaParams?>,
    lifecycleOwner: LifecycleOwner
) {
    lifecycleOwner.lifecycleScope.launch {
        passiveCaptchaParamsFlow
            .mapNotNull { it }
            .take(1)
            .collect {
                bootstrapHelper(it, lifecycleOwner)
            }
    }
}
