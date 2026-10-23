package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ConsumerFixtures
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.LinkBrand
import org.json.JSONArray
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
                supportedPaymentDetailsTypes = listOf("CARD"),
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
                supportedPaymentDetailsTypes = listOf("CARD"),
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
                supportedPaymentDetailsTypes = listOf("CARD"),
            )
        )
    }

    @Test
    fun `Parse consumer with link_brand onelink`() {
        val json = JSONObject(
            """
                {
                  "auth_session_client_secret": null,
                  "link_brand": "onelink",
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
        assertThat(result?.linkBrand).isEqualTo(LinkBrand.Onelink)
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

    @Test
    fun `Parse consumer with support_payment_details_types`() {
        val json = JSONObject(
            """
                {
                  "consumer_session": {
                    "client_secret": "secret_123",
                    "email_address": "test@stripe.com",
                    "redacted_phone_number": "+1********56",
                    "redacted_formatted_phone_number": "(***) *** **56",
                    "verification_sessions": [],
                    "support_payment_details_types": ["CARD", "BANK_ACCOUNT"]
                  }
                }
            """.trimIndent()
        )
        val result = ConsumerSessionJsonParser().parse(json)
        assertThat(result?.supportedPaymentDetailsTypes).containsExactly("CARD", "BANK_ACCOUNT")
    }

    @Test
    fun `Parse consumer without support_payment_details_types returns empty list`() {
        val json = JSONObject(
            """
                {
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
        assertThat(result?.supportedPaymentDetailsTypes).isEmpty()
    }

    @Test
    fun `Parse consumer with link_session_key`() {
        val json = JSONObject(
            """
                {
                  "consumer_session": {
                    "client_secret": "secret_123",
                    "link_session_key": "lsk_123",
                    "email_address": "test@stripe.com",
                    "redacted_phone_number": "+1********56",
                    "redacted_formatted_phone_number": "(***) *** **56",
                    "verification_sessions": []
                  }
                }
            """.trimIndent()
        )

        val result = ConsumerSessionJsonParser().parse(json)

        assertThat(result?.linkSessionKey).isEqualTo("lsk_123")
    }

    @Test
    fun `Parse consumer without link_session_key returns null`() {
        val result = ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFIED_JSON)

        assertThat(result?.linkSessionKey).isNull()
    }

    @Test
    fun `Parse available verification factors, keeping unknown types`() {
        val json = JSONObject(ConsumerFixtures.CONSUMER_VERIFICATION_STARTED_JSON.toString())
        json.getJSONObject("consumer_session").put(
            "available_verification_factors",
            JSONArray(
                """
                    [
                      {"type": "SMS", "id": "casvf_1", "provides_further_verification": true,
                        "temporarily_disabled": false},
                      {"type": "EMAIL", "id": "casvf_2", "provides_further_verification": false,
                        "temporarily_disabled": true},
                      {"type": "PHONE_MATCH", "id": "casvf_3", "provides_further_verification": true,
                        "temporarily_disabled": false}
                    ]
                """.trimIndent()
            )
        )

        val factors = ConsumerSessionJsonParser().parse(json)?.availableVerificationFactors

        assertThat(factors).containsExactly(
            ConsumerSession.VerificationFactor(
                type = ConsumerSession.VerificationFactor.FactorType.Sms,
                id = "casvf_1",
                providesFurtherVerification = true,
                temporarilyDisabled = false,
            ),
            ConsumerSession.VerificationFactor(
                type = ConsumerSession.VerificationFactor.FactorType.Email,
                id = "casvf_2",
                providesFurtherVerification = false,
                temporarilyDisabled = true,
            ),
            ConsumerSession.VerificationFactor(
                type = ConsumerSession.VerificationFactor.FactorType.Unknown,
                id = "casvf_3",
                providesFurtherVerification = true,
                temporarilyDisabled = false,
            ),
        ).inOrder()
    }

    @Test
    fun `Missing verification factors parse as null`() {
        val session = ConsumerSessionJsonParser().parse(ConsumerFixtures.CONSUMER_VERIFICATION_STARTED_JSON)

        assertThat(session?.availableVerificationFactors).isNull()
        assertThat(session?.emailOtpRequiresAdditionalInfo).isNull()
    }

    @Test
    fun `Parse email OTP settings from top-level settings`() {
        val json = JSONObject(ConsumerFixtures.CONSUMER_VERIFICATION_STARTED_JSON.toString())
        json.put("settings", JSONObject().put("email_otp_requires_additional_info", false))

        val session = ConsumerSessionJsonParser().parse(json)

        assertThat(session?.emailOtpRequiresAdditionalInfo).isFalse()
    }

    @Test
    fun `Unknown authentication levels never meet the minimum`() {
        val json = JSONObject(ConsumerFixtures.CONSUMER_VERIFICATION_STARTED_JSON.toString())
        json.getJSONObject("consumer_session")
            .put("current_authentication_level", "2FA")
            .put("minimum_authentication_level", "3FA")

        val session = ConsumerSessionJsonParser().parse(json)

        assertThat(session?.meetsMinimumAuthenticationLevel).isFalse()
    }
}
