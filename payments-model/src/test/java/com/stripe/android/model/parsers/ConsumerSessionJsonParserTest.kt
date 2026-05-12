package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ConsumerFixtures
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import org.json.JSONObject
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
                unredactedPhoneNumber = null,
                phoneNumberCountry = null,
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
                unredactedPhoneNumber = null,
                phoneNumberCountry = null,
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
    fun `Parse consumer with authentication levels`() {
        assertThat(
            ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFIED_WITH_AUTH_LEVEL_JSON)
        ).isEqualTo(
            ConsumerSession(
                clientSecret = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDUzNTFkNjNhLTZkNGMtND",
                emailAddress = "test@stripe.com",
                redactedPhoneNumber = "+1********56",
                redactedFormattedPhoneNumber = "(***) *** **56",
                unredactedPhoneNumber = null,
                phoneNumberCountry = null,
                verificationSessions = emptyList(),
                currentAuthenticationLevel = ConsumerSession.AuthenticationLevel.OneFactorAuthentication,
                minimumAuthenticationLevel = ConsumerSession.AuthenticationLevel.OneFactorAuthentication,
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
                unredactedPhoneNumber = null,
                phoneNumberCountry = null,
                verificationSessions = listOf(
                    ConsumerSession.VerificationSession(
                        type = ConsumerSession.VerificationSession.SessionType.SignUp,
                        state = ConsumerSession.VerificationSession.SessionState.Started
                    )
                ),
            )
        )
    }

    @Test
    fun `Parse consumer with link_brand notlink`() {
        val json = JSONObject(
            """
                {
                  "auth_session_client_secret": null,
                  "link_brand": "notlink",
                  "consumer_session": {
                    "client_secret": "secret_123",
                    "email_address": "test@stripe.com",
                    "redacted_phone_number": "+1********56",
                    "redacted_formatted_phone_number": "(***) *** **56",
                    "verification_sessions": []
                  }
                }
            """.trimIndent()
        )
        val result = ConsumerSessionJsonParser().parse(json)
        assertThat(result?.linkBrand).isEqualTo(LinkBrand.Notlink)
    }

    @Test
    fun `Parse consumer with link_brand link`() {
        val json = JSONObject(
            """
                {
                  "auth_session_client_secret": null,
                  "link_brand": "link",
                  "consumer_session": {
                    "client_secret": "secret_123",
                    "email_address": "test@stripe.com",
                    "redacted_phone_number": "+1********56",
                    "redacted_formatted_phone_number": "(***) *** **56",
                    "verification_sessions": []
                  }
                }
            """.trimIndent()
        )
        val result = ConsumerSessionJsonParser().parse(json)
        assertThat(result?.linkBrand).isEqualTo(LinkBrand.Link)
    }

    @Test
    fun `Parse consumer without link_brand field returns null linkBrand`() {
        val result = ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFIED_JSON)
        assertThat(result?.linkBrand).isNull()
    }

    @Test
    fun `Parse consumer with unknown link_brand value returns null linkBrand`() {
        val json = JSONObject(
            """
                {
                  "auth_session_client_secret": null,
                  "link_brand": "unknown_brand",
                  "consumer_session": {
                    "client_secret": "secret_123",
                    "email_address": "test@stripe.com",
                    "redacted_phone_number": "+1********56",
                    "redacted_formatted_phone_number": "(***) *** **56",
                    "verification_sessions": []
                  }
                }
            """.trimIndent()
        )
        val result = ConsumerSessionJsonParser().parse(json)
        assertThat(result?.linkBrand).isNull()
    }
}
