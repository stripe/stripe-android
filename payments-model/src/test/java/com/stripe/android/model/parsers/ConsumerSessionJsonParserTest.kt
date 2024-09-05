package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ConsumerFixtures
import com.stripe.android.model.ConsumerSession
import org.junit.Test

class ConsumerSessionJsonParserTest {

    @Test
    fun `Parse consumer when verification started`() {
        assertThat(
            ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFICATION_STARTED_JSON)
        ).isEqualTo(
            ConsumerSession(
                clientSecret = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDNmZDE1MjA5LTM1YjctND",
                emailAddress = "test@stripe.com",
                redactedPhoneNumber = "+1********56",
                redactedFormattedPhoneNumber = "(***) *** **56",
                verificationSessions = listOf(
                    ConsumerSession.VerificationSession(
                        type = ConsumerSession.VerificationSession.SessionType.Sms,
                        state = ConsumerSession.VerificationSession.SessionState.Started
                    )
                ),
            )
        )
    }

    @Test
    fun `Parse verified consumer`() {
        assertThat(
            ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFIED_JSON)
        ).isEqualTo(
            ConsumerSession(
                clientSecret = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDUzNTFkNjNhLTZkNGMtND",
                emailAddress = "test@stripe.com",
                redactedPhoneNumber = "+1********56",
                redactedFormattedPhoneNumber = "(***) *** **56",
                verificationSessions = listOf(
                    ConsumerSession.VerificationSession(
                        type = ConsumerSession.VerificationSession.SessionType.Sms,
                        state = ConsumerSession.VerificationSession.SessionState.Verified
                    )
                ),
            )
        )
    }

    @Test
    fun `Parse consumer when signup started`() {
        assertThat(
            ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_SIGNUP_STARTED_JSON)
        ).isEqualTo(
            ConsumerSession(
                clientSecret = "12oBEhVjc21yKkFYNmNWT0JmaFFBQUFLUXcaJDk5OGFjYTFlLTkxMWYtND",
                emailAddress = "test@stripe.com",
                redactedPhoneNumber = "+1********23",
                redactedFormattedPhoneNumber = "(***) *** **23",
                verificationSessions = listOf(
                    ConsumerSession.VerificationSession(
                        type = ConsumerSession.VerificationSession.SessionType.SignUp,
                        state = ConsumerSession.VerificationSession.SessionState.Started
                    )
                ),
            )
        )
    }
}
