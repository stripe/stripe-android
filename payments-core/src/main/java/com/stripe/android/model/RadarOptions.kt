package com.stripe.android.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class RadarOptions(
    val hCaptchaToken: String
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_HCAPTCHA_TOKEN to hCaptchaToken
        )
    }

    private companion object {
        const val PARAM_HCAPTCHA_TOKEN = "hcaptcha_token"
    }
}
