package com.stripe.android.link.model

import android.os.Parcelable
import com.stripe.android.model.ConsumerSession
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Immutable object representing a Link account.
 */
@Parcelize
internal class LinkAccount(private val consumerSession: ConsumerSession) : Parcelable {

    @IgnoredOnParcel
    val redactedPhoneNumber = consumerSession.redactedPhoneNumber

    @IgnoredOnParcel
    val clientSecret = consumerSession.clientSecret

    @IgnoredOnParcel
    val email = consumerSession.emailAddress

    @IgnoredOnParcel
    val isVerified: Boolean = consumerSession.containsVerifiedSMSSession() ||
        consumerSession.isVerifiedForSignup()

    @IgnoredOnParcel
    val accountStatus = when {
        isVerified -> {
            AccountStatus.Verified
        }
        consumerSession.containsSMSSessionStarted() -> {
            AccountStatus.VerificationStarted
        }
        else -> {
            AccountStatus.NeedsVerification
        }
    }

    private fun ConsumerSession.containsSMSSessionStarted() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.Sms &&
            it.state == ConsumerSession.VerificationSession.SessionState.Started
    } != null

    private fun ConsumerSession.containsVerifiedSMSSession() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.Sms &&
            it.state == ConsumerSession.VerificationSession.SessionState.Verified
    } != null

    private fun ConsumerSession.isVerifiedForSignup() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.SignUp &&
            it.state == ConsumerSession.VerificationSession.SessionState.Started
    } != null
}
