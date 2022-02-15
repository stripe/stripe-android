package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * The result of a call to Link consumer sign up.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsumerSession internal constructor(
    val clientSecret: String,
    val emailAddress: String,
    val redactedPhoneNumber: String,
    val verificationSessions: List<VerificationSession>
) : StripeModel {

    @Parcelize
    data class VerificationSession internal constructor(
        val type: SessionType,
        val state: SessionState
    ) : StripeModel {

        @Parcelize
        enum class SessionType(val value: String) : Parcelable {
            Unknown(""),
            SignUp("signup"),
            Email("email"),
            Sms("sms");

            companion object {
                fun fromValue(value: String): SessionType =
                    values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
            }
        }

        @Parcelize
        enum class SessionState(val value: String) : Parcelable {
            Unknown(""),
            Started("started"),
            Failed("failed"),
            Verified("verified"),
            Canceled("canceled"),
            Expired("expired");

            companion object {
                fun fromValue(value: String): SessionState =
                    values().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: Unknown
            }
        }
    }
}
