package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

sealed interface BootstrapKey<Value : Parcelable> {
    fun toValue(metadata: Map<BootstrapKey<*>, Parcelable>): Value?

    data object PassiveCaptcha : BootstrapKey<PassiveCaptchaParams> {
        override fun toValue(metadata: Map<BootstrapKey<*>, Parcelable>): PassiveCaptchaParams? {
            if (metadata.containsKey(this).not()) return null
            return (metadata[this] as? PassiveCaptchaParams)
                ?: throw IllegalArgumentException("invalid value for ${this::class.simpleName}")
        }
    }
}

internal fun ConfirmationHandler.bootstrap(
    passiveCaptchaParams: PassiveCaptchaParams,
    lifecycleOwner: LifecycleOwner
) {
    bootstrap(mapOf(BootstrapKey.PassiveCaptcha to passiveCaptchaParams), lifecycleOwner)
}

internal fun ConfirmationHandler.bootstrap(
    passiveCaptchaParamsFlow: Flow<PassiveCaptchaParams?>,
    lifecycleOwner: LifecycleOwner
) {
    lifecycleOwner.lifecycleScope.launch {
        passiveCaptchaParamsFlow
            .mapNotNull { it }
            .take(1)
            .collect {
                bootstrap(it, lifecycleOwner)
            }
    }
}
