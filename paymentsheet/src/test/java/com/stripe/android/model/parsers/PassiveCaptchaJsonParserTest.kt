package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PassiveCaptchaParams
import org.json.JSONObject
import kotlin.test.Test

class PassiveCaptchaJsonParserTest {
    private val parser = PassiveCaptchaJsonParser()

    @Test
    fun `parse returns PassiveCaptchaParams when site_key is present`() {
        val result = parser.parse(PASSIVE_CAPTCHA_JSON_WITH_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = "test_rq_data",
                tokenTimeoutSeconds = 30
            )
        )
    }

    @Test
    fun `parse returns PassiveCaptchaParams with null rq_data when rq_data is missing`() {
        val result = parser.parse(PASSIVE_CAPTCHA_JSON_WITHOUT_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = null,
                tokenTimeoutSeconds = null
            )
        )
    }

    @Test
    fun `parse returns PassiveCaptchaParams with null rq_data when rq_data is blank`() {
        val result = parser.parse(PASSIVE_CAPTCHA_JSON_WITH_BLANK_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = null,
                tokenTimeoutSeconds = null
            )
        )
    }

    @Test
    fun `parse returns null when site_key is missing`() {
        val result = parser.parse(PASSIVE_CAPTCHA_JSON_MISSING_SITE_KEY)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns PassiveCaptchaParams with null rq_data when rq_data is string null`() {
        val result = parser.parse(PASSIVE_CAPTCHA_JSON_WITH_NULL_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = null,
                tokenTimeoutSeconds = null
            )
        )
    }

    @Test
    fun `parse returns null when JSON is empty`() {
        val result = parser.parse(JSONObject("{}"))

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns PassiveCaptchaParams with tokenTimeoutSeconds when present`() {
        val result = parser.parse(PASSIVE_CAPTCHA_JSON_WITH_TOKEN_TIMEOUT)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = null,
                tokenTimeoutSeconds = 60
            )
        )
    }

    private companion object {
        val PASSIVE_CAPTCHA_JSON_WITH_RQ_DATA = JSONObject(
            """
            {
              "site_key": "test_site_key",
              "rqdata": "test_rq_data",
              "token_timeout_seconds": 30
            }
            """.trimIndent()
        )

        val PASSIVE_CAPTCHA_JSON_WITHOUT_RQ_DATA = JSONObject(
            """
            {
              "site_key": "test_site_key"
            }
            """.trimIndent()
        )

        val PASSIVE_CAPTCHA_JSON_WITH_TOKEN_TIMEOUT = JSONObject(
            """
            {
              "site_key": "test_site_key",
              "token_timeout_seconds": 60
            }
            """.trimIndent()
        )

        val PASSIVE_CAPTCHA_JSON_WITH_BLANK_RQ_DATA = JSONObject(
            """
            {
              "site_key": "test_site_key",
              "rqdata": ""
            }
            """.trimIndent()
        )

        val PASSIVE_CAPTCHA_JSON_WITH_NULL_RQ_DATA = JSONObject(
            """
            {
              "site_key": "test_site_key",
              "rqdata": "null"
            }
            """.trimIndent()
        )

        val PASSIVE_CAPTCHA_JSON_MISSING_SITE_KEY = JSONObject(
            """
            {
              "rqdata": "test_rq_data"
            }
            """.trimIndent()
        )
    }
}
