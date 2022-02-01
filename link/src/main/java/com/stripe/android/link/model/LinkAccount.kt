package com.stripe.android.link.model

import com.stripe.android.model.ConsumerSession

/**
 * Represents a Link Account, keeping track of its state across API calls.
 */
internal class LinkAccount(consumerSession: ConsumerSession) {
    val isVerified: Boolean =
        consumerSession.verificationSessions.find {
            it.type == ConsumerSession.VerificationSession.SessionType.Sms &&
                it.state == ConsumerSession.VerificationSession.SessionState.Verified
        } != null
}
