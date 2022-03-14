package com.stripe.android.link.model

import com.stripe.android.model.ConsumerSession

/**
 * Immutable object representing a Link account.
 */
internal class LinkAccount(private val consumerSession: ConsumerSession) {

    val redactedPhoneNumber = consumerSession.redactedPhoneNumber
    val clientSecret = consumerSession.clientSecret

    val isVerified: Boolean = consumerSession.containsVerifiedSMSSession() ||
        consumerSession.isVerifiedForSignup()

    fun getAuthSessionCookie() = consumerSession.authSessionClientSecret

    private fun ConsumerSession.containsVerifiedSMSSession() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.Sms &&
            it.state == ConsumerSession.VerificationSession.SessionState.Verified
    } != null

    private fun ConsumerSession.isVerifiedForSignup() = verificationSessions.find {
        it.type == ConsumerSession.VerificationSession.SessionType.SignUp &&
            it.state == ConsumerSession.VerificationSession.SessionState.Started
    } != null
}
