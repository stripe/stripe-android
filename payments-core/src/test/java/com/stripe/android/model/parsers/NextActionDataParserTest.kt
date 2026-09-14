package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
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
                        "captcha_vendor_name": "arkose"
                    }
                }
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)

        assertThat(nextActionData)
            .isEqualTo(
                StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge(
                    stripeJs = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.StripeJs(
                        captchaVendorName = "arkose"
                    )
                )
            )
    }

    @Test
    fun `parse with captcha_vendor_name null results in null`() {
        val nextActionJson = JSONObject(
            """
            {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "intent_confirmation_challenge",
                    "stripe_js": {
                        "captcha_vendor_name": null
                    }
                }
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)

        val challenge = nextActionData as StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge
        assertThat(challenge.stripeJs.captchaVendorName).isNull()
    }

    @Test
    fun `parse with stripe_js missing results in null`() {
        val nextActionJson = JSONObject(
            """
            {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "intent_confirmation_challenge"
                }
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)

        val challenge = nextActionData as StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge
        assertThat(challenge.stripeJs.captchaVendorName).isNull()
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

    @Test
    fun `parse with alipay_handle_redirect and native_data present should create AlipayRedirect with data`() {
        val nextActionJson = JSONObject(
            """
            {
                "type": "alipay_handle_redirect",
                "alipay_handle_redirect": {
                    "native_data": "native_data_value",
                    "url": "https://example.com/alipay"
                }
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)
        assertThat(nextActionData).isInstanceOf<StripeIntent.NextActionData.AlipayRedirect>()
        val alipayRedirect = nextActionData as StripeIntent.NextActionData.AlipayRedirect
        assertThat(alipayRedirect.type).isInstanceOf<StripeIntent.NextActionData.AlipayRedirect.Type.WithNativeData>()
        val type = alipayRedirect.type as StripeIntent.NextActionData.AlipayRedirect.Type.WithNativeData
        assertThat(type.data).isEqualTo("native_data_value")
    }

    @Test
    fun `parse with alipay_handle_redirect and native_data missing should create default AlipayRedirect`() {
        val nextActionJson = JSONObject(
            """
            {
                "type": "alipay_handle_redirect",
                "alipay_handle_redirect": {
                    "url": "https://example.com/alipay"
                }
            }
            """.trimIndent()
        )

        val nextActionData = NextActionDataParser().parse(nextActionJson)
        assertThat(nextActionData).isInstanceOf<StripeIntent.NextActionData.AlipayRedirect>()
        val alipayRedirect = nextActionData as StripeIntent.NextActionData.AlipayRedirect
        assertThat(alipayRedirect.type).isEqualTo(StripeIntent.NextActionData.AlipayRedirect.Type.Default)
    }
}
