package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class VerifyIntentConfirmationChallengeParams(
    val clientSecret: String,
    val captchaVendorName: String,
    val challengeResponseEkey: String? = null,
    val challengeResponseToken: String? = null,
    val px3: String? = null,
    val pxcts: String? = null,
    val pxvid: String? = null,
    val arkoseToken: String? = null,
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_CLIENT_SECRET to clientSecret,
            PARAM_CAPTCHA_VENDOR_NAME to captchaVendorName,
        ).plus(
            challengeResponseEkey?.let { mapOf(PARAM_CHALLENGE_RESPONSE_EKEY to it) }.orEmpty()
        ).plus(
            challengeResponseToken?.let { mapOf(PARAM_CHALLENGE_RESPONSE_TOKEN to it) }.orEmpty()
        ).plus(
            px3?.let { mapOf(PARAM_PX3 to it) }.orEmpty()
        ).plus(
            pxcts?.let { mapOf(PARAM_PXCTS to it) }.orEmpty()
        ).plus(
            pxvid?.let { mapOf(PARAM_PXVID to it) }.orEmpty()
        ).plus(
            arkoseToken?.let { mapOf(PARAM_ARKOSE_TOKEN to it) }.orEmpty()
        )
    }

    private companion object {
        private const val PARAM_CLIENT_SECRET = "client_secret"
        private const val PARAM_CAPTCHA_VENDOR_NAME = "captcha_vendor_name"
        private const val PARAM_CHALLENGE_RESPONSE_EKEY = "challenge_response_ekey"
        private const val PARAM_CHALLENGE_RESPONSE_TOKEN = "challenge_response_token"
        private const val PARAM_PX3 = "px3"
        private const val PARAM_PXCTS = "pxcts"
        private const val PARAM_PXVID = "pxvid"
        private const val PARAM_ARKOSE_TOKEN = "arkose_token"
    }
}
