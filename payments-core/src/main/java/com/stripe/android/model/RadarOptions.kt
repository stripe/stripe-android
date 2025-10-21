package com.stripe.android.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class RadarOptions(
    private val hCaptchaToken: String?,
    private val androidVerificationObject: AndroidVerificationObject?
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return hCaptchaToken?.let {
            mapOf(PARAM_HCAPTCHA_TOKEN to it)
        }.orEmpty()
            .plus(
                androidVerificationObject?.let {
                    mapOf(PARAM_ANDROID_VERIFICATION_OBJECT to it.toParamMap())
                }.orEmpty()
            )
    }

    private companion object {
        const val PARAM_HCAPTCHA_TOKEN = "hcaptcha_token"
        const val PARAM_ANDROID_VERIFICATION_OBJECT = "android_verification_object"
    }
}
