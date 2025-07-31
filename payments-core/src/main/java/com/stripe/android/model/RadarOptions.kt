package com.stripe.android.model

import kotlinx.parcelize.Parcelize

@Parcelize
data class RadarOptions(
    val hCaptchaToken: String
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_HCAPTCHA_TOKEN to hCaptchaToken
        )
    }

    companion object {
        const val PARAM_HCAPTCHA_TOKEN = "hcaptcha_token"
    }
}
