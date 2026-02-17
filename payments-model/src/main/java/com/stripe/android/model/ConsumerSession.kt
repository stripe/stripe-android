package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsumerSession(
    @SerialName("client_secret")
    val clientSecret: String = "",
    @SerialName("email_address")
    val emailAddress: String,
    @SerialName("redacted_formatted_phone_number")
    val redactedFormattedPhoneNumber: String,
    @SerialName("redacted_phone_number")
    val redactedPhoneNumber: String,
    @SerialName("unredacted_phone_number")
    val unredactedPhoneNumber: String? = null,
    @SerialName("phone_number_country")
    val phoneNumberCountry: String? = null,
    @SerialName("verification_sessions")
    val verificationSessions: List<VerificationSession> = emptyList(),
    @SerialName("mobile_fallback_webview_params")
    val mobileFallbackWebviewParams: MobileFallbackWebviewParams? = null,
    @SerialName("current_authentication_level")
    val currentAuthenticationLevel: AuthenticationLevel? = null,
    @SerialName("minimum_authentication_level")
    val minimumAuthenticationLevel: AuthenticationLevel? = null,
) : StripeModel {

    val meetsMinimumAuthenticationLevel: Boolean
        get() {
            val current = currentAuthenticationLevel ?: return false
            val minimum = minimumAuthenticationLevel ?: return false
            return current.sortOrder >= minimum.sortOrder
        }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    data class VerificationSession(
        val type: SessionType,
        val state: SessionState
    ) : StripeModel {

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class SessionType(val value: String) : Parcelable {
            Unknown(""),
            LinkAuthToken("link_auth_token"),
            SignUp("signup"),
            Email("email"),
            Sms("sms");

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            companion object {
                fun fromValue(value: String): SessionType =
                    entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
            }
        }

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class SessionState(val value: String) : Parcelable {
            Unknown(""),
            Started("started"),
            Failed("failed"),
            Verified("verified"),
            Canceled("canceled"),
            Expired("expired");

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            companion object {
                fun fromValue(value: String): SessionState =
                    entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
            }
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    enum class AuthenticationLevel(val value: String, val sortOrder: Int) : Parcelable {
        Unknown("", -1),
        NotAuthenticated("not_authenticated", 0),
        OneFactorAuthentication("1fa", 1),
        TwoFactorAuthentication("2fa", 2);

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            fun fromValue(value: String): AuthenticationLevel =
                entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
        }
    }
}
