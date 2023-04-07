package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * The result of a call to Link consumer sign up.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsumerSession(
    val clientSecret: String,
    val emailAddress: String,
    val redactedPhoneNumber: String,
    val verificationSessions: List<VerificationSession>,
    val authSessionClientSecret: String?,
    val publishableKey: String?
) : StripeModel {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    data class VerificationSession constructor(
        val type: SessionType,
        val state: SessionState
    ) : StripeModel {

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class SessionType(val value: String) : Parcelable {
            Unknown(""),
            SignUp("signup"),
            Email("email"),
            Sms("sms");

            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            companion object {
                fun fromValue(value: String): SessionType =
                    values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
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
                    values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
            }
        }
    }
}
