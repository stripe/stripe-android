package com.stripe.hcaptcha.config

import com.stripe.hcaptcha.encode.encodeToJson
import org.junit.Test
import kotlin.test.assertEquals

class HCaptchaSizeTest {
    @Test
    fun serializes_to_json_value() {
        assertEquals("\"invisible\"", encodeToJson(HCaptchaSize.serializer(), HCaptchaSize.INVISIBLE))
        assertEquals("\"compact\"", encodeToJson(HCaptchaSize.serializer(), HCaptchaSize.COMPACT))
        assertEquals("\"normal\"", encodeToJson(HCaptchaSize.serializer(), HCaptchaSize.NORMAL))
    }
}
