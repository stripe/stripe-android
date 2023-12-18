package com.stripe.hcaptcha.config

import com.stripe.hcaptcha.encode.encodeToJson
import org.junit.Test
import kotlin.test.assertEquals

class HCaptchaThemeTest {
    @Test
    fun serializes_to_json_value() {
        assertEquals("\"dark\"", encodeToJson(HCaptchaTheme.serializer(), HCaptchaTheme.DARK))
        assertEquals("\"light\"", encodeToJson(HCaptchaTheme.serializer(), HCaptchaTheme.LIGHT))
        assertEquals("\"contrast\"", encodeToJson(HCaptchaTheme.serializer(), HCaptchaTheme.CONTRAST))
    }
}
