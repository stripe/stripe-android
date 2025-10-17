package com.stripe.android.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class AndroidVerificationObject(
    val androidVerificationToken: String?
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return androidVerificationToken?.let {
            mapOf(
                PARAM_ANDROID_VERIFICATION_TOKEN to it
            )
        }.orEmpty()
    }

    private companion object {
        const val PARAM_ANDROID_VERIFICATION_TOKEN = "android_verification_token"
    }
}
