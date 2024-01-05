package com.stripe.hcaptcha

import com.stripe.hcaptcha.HCaptchaError.Companion.fromId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HCaptchaErrorTest {
    @Test
    fun enum_codes() {
        assertEquals("No internet connection", HCaptchaError.NETWORK_ERROR.toString())
        assertEquals("Session Timeout", HCaptchaError.SESSION_TIMEOUT.toString())
        assertEquals("Challenge Closed", HCaptchaError.CHALLENGE_CLOSED.toString())
        assertEquals("Rate Limited", HCaptchaError.RATE_LIMITED.toString())
        assertEquals("Unknown error", HCaptchaError.ERROR.toString())
    }

    @Test
    fun enum_ids() {
        assertEquals(7, HCaptchaError.NETWORK_ERROR.errorId.toLong())
        assertEquals(15, HCaptchaError.SESSION_TIMEOUT.errorId.toLong())
        assertEquals(30, HCaptchaError.CHALLENGE_CLOSED.errorId.toLong())
        assertEquals(31, HCaptchaError.RATE_LIMITED.errorId.toLong())
        assertEquals(29, HCaptchaError.ERROR.errorId.toLong())
    }

    @Test
    fun get_enum_from_id() {
        assertEquals(HCaptchaError.NETWORK_ERROR, fromId(7))
        assertEquals(HCaptchaError.SESSION_TIMEOUT, fromId(15))
        assertEquals(HCaptchaError.CHALLENGE_CLOSED, fromId(30))
        assertEquals(HCaptchaError.RATE_LIMITED, fromId(31))
        assertEquals(HCaptchaError.ERROR, fromId(29))
    }

    @Test
    fun get_enum_from_invalid_id_throws() {
        assertFailsWith(RuntimeException::class) { fromId(-999) }
    }
}
