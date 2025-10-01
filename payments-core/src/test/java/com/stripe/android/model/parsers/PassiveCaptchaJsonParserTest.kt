package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ElementsSessionFixtures
import com.stripe.android.model.PassiveCaptchaParams
import org.json.JSONObject
import kotlin.test.Test

class PassiveCaptchaJsonParserTest {
    private val parser = PassiveCaptchaJsonParser()

    @Test
    fun `parse returns PassiveCaptchaParams when site_key is present`() {
        val result = parser.parse(ElementsSessionFixtures.PASSIVE_CAPTCHA_JSON_WITH_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = "test_rq_data"
            )
        )
    }

    @Test
    fun `parse returns PassiveCaptchaParams with null rq_data when rq_data is missing`() {
        val result = parser.parse(ElementsSessionFixtures.PASSIVE_CAPTCHA_JSON_WITHOUT_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = null
            )
        )
    }

    @Test
    fun `parse returns PassiveCaptchaParams with null rq_data when rq_data is blank`() {
        val result = parser.parse(ElementsSessionFixtures.PASSIVE_CAPTCHA_JSON_WITH_BLANK_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = null
            )
        )
    }

    @Test
    fun `parse returns null when site_key is missing`() {
        val result = parser.parse(ElementsSessionFixtures.PASSIVE_CAPTCHA_JSON_MISSING_SITE_KEY)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns PassiveCaptchaParams with null rq_data when rq_data is string null`() {
        val result = parser.parse(ElementsSessionFixtures.PASSIVE_CAPTCHA_JSON_WITH_NULL_RQ_DATA)

        assertThat(result).isEqualTo(
            PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = null
            )
        )
    }

    @Test
    fun `parse returns null when JSON is empty`() {
        val result = parser.parse(JSONObject("{}"))

        assertThat(result).isNull()
    }
}
