package com.stripe.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiRequestOptionsTest {

    @Test
    fun testCreate() {
        val opts = ApiRequest.Options(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "account")
        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, opts.apiKey)
        assertEquals("account", opts.stripeAccount)
    }

    @Test
    fun testCreate_withSecretKey_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ApiRequest.Options(ApiKeyFixtures.FAKE_SECRET_KEY)
        }
    }
}
