package com.stripe.android.hcaptcha

internal interface IsHCaptchaAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsHCaptchaAvailable : IsHCaptchaAvailable {
    override fun invoke(): Boolean {
        return try {
            Class.forName("com.hcaptcha.sdk.HCaptcha")
            true
        } catch (_: Exception) {
            false
        }
    }
}
