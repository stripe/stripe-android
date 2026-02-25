package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class NextActionDataParserTest {

    @Test
    fun `parse with intent_confirmation_challenge type should create IntentConfirmationChallenge`() {
        val nextActionJson = JSONObject(
            """
            {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "intent_confirmation_challenge",
                    "stripe_js": {
                        "site_key": "test_site_key",
                        "verification_url": "/v1/payment_intents/pi_123/verify_challenge"
                    }
                }
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)

        assertThat(nextActionData).isInstanceOf(
            StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge::class.java
        )
        val challenge = nextActionData as StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge
        assertThat(challenge.stripeJs.siteKey).isEqualTo("test_site_key")
        assertThat(challenge.stripeJs.verificationUrl).isEqualTo("/v1/payment_intents/pi_123/verify_challenge")
    }

    @Test
    fun `parse with unknown sdk type should return null`() {
        val nextActionJson = JSONObject(
            """
            {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "unknown_type"
                }
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)

        assertThat(nextActionData).isNull()
    }

    @Test
    fun `parse with missing type field should return null`() {
        val nextActionJson = JSONObject(
            """
            {
                "type": "unknown_action"
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)

        assertThat(nextActionData).isNull()
    }
}
