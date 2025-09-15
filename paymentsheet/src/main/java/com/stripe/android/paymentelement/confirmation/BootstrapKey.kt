package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import com.stripe.android.model.PassiveCaptchaParams

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
