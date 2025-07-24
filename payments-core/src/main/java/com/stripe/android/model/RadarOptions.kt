package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadarOptions(
    val hcaptchaToken: String
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_HCAPTCHA_TOKEN to hcaptchaToken
        )
    }

    companion object {
        const val PARAM_HCAPTCHA_TOKEN = "hcaptcha_token"
    }
}
