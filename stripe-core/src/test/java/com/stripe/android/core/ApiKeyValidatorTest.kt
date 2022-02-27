package com.stripe.android.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiKeyValidatorTest {

    @Test
    fun testPublishableKey() {
        assertEquals(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            ApiKeyValidator.get().requireValid(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )
    }

    @Test
    fun testEphemeralKey() {
        assertEquals(
            ApiKeyFixtures.FAKE_EPHEMERAL_KEY,
            ApiKeyValidator.get().requireValid(ApiKeyFixtures.FAKE_EPHEMERAL_KEY)
        )
    }

    @Test
    fun testSecretKey_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ApiKeyValidator.get().requireValid(ApiKeyFixtures.FAKE_SECRET_KEY)
        }
    }

    @Test
    fun testEmpty_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ApiKeyValidator.get().requireValid("   ")
        }
    }

    @Test
    fun testNull_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ApiKeyValidator.get().requireValid(null)
        }
    }
}
