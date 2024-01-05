package com.stripe.hcaptcha

import org.junit.Test
import kotlin.test.assertEquals

class HCaptchaExceptionTest {
    @Test
    fun exception_matches_error() {
        val error = HCaptchaError.NETWORK_ERROR

        try {
            throw HCaptchaException(error)
        } catch (hCaptchaException: HCaptchaException) {
            assertEquals(error.errorId.toLong(), hCaptchaException.statusCode.toLong())
            assertEquals(error.message, hCaptchaException.message)
            assertEquals(error, hCaptchaException.hCaptchaError)
        }
    }
}
