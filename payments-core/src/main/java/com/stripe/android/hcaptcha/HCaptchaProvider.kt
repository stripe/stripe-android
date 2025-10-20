package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.HCaptcha

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface HCaptchaProvider {
    fun get(): HCaptcha
}

internal class DefaultHCaptchaProvider : HCaptchaProvider {
    override fun get(): HCaptcha {
        return HCaptcha.getClient()
    }
}
