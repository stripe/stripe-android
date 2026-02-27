package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CancelCaptchaChallengeParams(
    val clientSecret: String,
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_CLIENT_SECRET to clientSecret,
        )
    }

    private companion object {
        private const val PARAM_CLIENT_SECRET = "client_secret"
    }
}
