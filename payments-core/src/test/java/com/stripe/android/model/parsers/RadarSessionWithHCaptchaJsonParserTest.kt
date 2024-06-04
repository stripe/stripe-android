package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.RadarSessionWithHCaptcha
import org.json.JSONObject
import kotlin.test.Test

class RadarSessionWithHCaptchaJsonParserTest {

    @Test
    fun `parse should return expected object`() {
        assertThat(RadarSessionWithHCaptchaJsonParser().parse(JSON))
            .isEqualTo(
                RadarSessionWithHCaptcha(
                    id = "rse_abc123",
                    passiveCaptchaSiteKey = null,
                    passiveCaptchaRqdata = null
                )
            )
    }

    private companion object {
        private val JSON = JSONObject(
            """
            {"id": "rse_abc123"}
            """.trimIndent()
        )
    }
}
