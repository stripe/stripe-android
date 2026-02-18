package com.stripe.android.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class AndroidVerificationObject(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val appId: String,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val androidVerificationToken: String
) : StripeParamsModel {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_ANDROID_VERIFICATION_TOKEN to androidVerificationToken,
            PARAM_APP_ID to appId
        )
    }

    private companion object {
        const val PARAM_APP_ID = "app_id"

        const val PARAM_ANDROID_VERIFICATION_TOKEN = "android_verification_token"
    }
}
