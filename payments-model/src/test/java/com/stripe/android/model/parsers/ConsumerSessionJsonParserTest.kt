package com.stripe.android.model.parsers

import com.stripe.android.ConsumerFixtures
import com.stripe.android.model.ConsumerSession
import org.junit.Test
import kotlin.test.assertEquals

class ConsumerSessionJsonParserTest {

    @Test
    fun `Parse consumer when verification started`() {
        assertEquals(
            ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFICATION_STARTED_JSON),
            ConsumerSession(
                clientSecret = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDNmZDE1MjA5LTM1YjctND",
                emailAddress = "test@stripe.com",
                redactedPhoneNumber = "+1********56",
                verificationSessions = listOf(
                    ConsumerSession.VerificationSession(
                        type = ConsumerSession.VerificationSession.SessionType.Sms,
                        state = ConsumerSession.VerificationSession.SessionState.Started
                    )
                ),
                authSessionClientSecret = "21yKkFYNnhMVTlXbXdBQUFJRmEaJDNmZDE1",
                publishableKey = "asdfg123"
            )
        )
    }

    @Test
    fun `Parse verified consumer`() {
        assertEquals(
            ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFIED_JSON),
            ConsumerSession(
                clientSecret = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDUzNTFkNjNhLTZkNGMtND",
                emailAddress = "test@stripe.com",
                redactedPhoneNumber = "+1********56",
                verificationSessions = listOf(
                    ConsumerSession.VerificationSession(
                        type = ConsumerSession.VerificationSession.SessionType.Sms,
                        state = ConsumerSession.VerificationSession.SessionState.Verified
                    )
                ),
                authSessionClientSecret = null,
                publishableKey = null
            )
        )
    }

    @Test
    fun `Parse consumer when signup started`() {
        assertEquals(
            ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_SIGNUP_STARTED_JSON),
            ConsumerSession(
                clientSecret = "12oBEhVjc21yKkFYNmNWT0JmaFFBQUFLUXcaJDk5OGFjYTFlLTkxMWYtND",
                emailAddress = "test@stripe.com",
                redactedPhoneNumber = "+1********23",
                verificationSessions = listOf(
                    ConsumerSession.VerificationSession(
                        type = ConsumerSession.VerificationSession.SessionType.SignUp,
                        state = ConsumerSession.VerificationSession.SessionState.Started
                    )
                ),
                authSessionClientSecret = null,
                publishableKey = null
            )
        )
    }
}
