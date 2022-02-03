package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * The result of a call to retrieve the [ConsumerSession] for a Link user.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsumerSessionLookup internal constructor(
    val exists: Boolean,
    val consumerSession: ConsumerSession?,
    val errorMessage: String?
) : StripeModel {

    @Parcelize
    data class ConsumerSession internal constructor(
        val clientSecret: String,
        val emailAddress: String,
        val redactedPhoneNumber: String,
        val verificationSessions: List<VerificationSession>
    ) : StripeModel

    @Parcelize
    data class VerificationSession internal constructor(
        val type: SessionType,
        val state: SessionState
    ) : StripeModel {

        @Parcelize
        enum class SessionType(val value: String) : Parcelable {
            Unknown(""),
            SignUp("sign_up"),
            Email("email"),
            Sms("sms")
        }

        @Parcelize
        enum class SessionState(val value: String) : Parcelable {
            Unknown(""),
            Started("started"),
            Failed("failed"),
            Verified("verified"),
            Canceled("canceled"),
            Expired("expired")
        }
    }
}
