package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.HCaptcha

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface HCaptchaProvider {
    fun get(activity: FragmentActivity): HCaptcha
}

internal class DefaultHCaptchaProvider : HCaptchaProvider {
    override fun get(activity: FragmentActivity): HCaptcha {
        return HCaptcha.getClient(activity)
    }
}
