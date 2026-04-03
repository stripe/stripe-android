package com.stripe.android.core.networking

import com.stripe.android.core.ApiKeyFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class ApiRequestTest {
    @Test
    fun getContentType() {
        val contentType = FACTORY.createGet(
            SOURCES_URL,
            OPTIONS
        ).postHeaders?.get(HEADER_CONTENT_TYPE)
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", contentType)
    }

    @Test
    fun writeBody_withEmptyBody_shouldHaveZeroLength() {
        ByteArrayOutputStream().use {
            FACTORY.createPost(
                PAYMENT_METHODS_URL,
                OPTIONS
            ).writePostBody(it)
            assertTrue(it.size() == 0)
        }
    }

    @Test
    fun writeBody_withNonEmptyBody_shouldHaveNonZeroLength() {
        ByteArrayOutputStream().use {
            FACTORY.createPost(
                PAYMENT_METHODS_URL,
                OPTIONS,
                mapOf("customer" to "cus_123")
            ).writePostBody(it)
            assertEquals(16, it.size())
        }
    }

    @Test
    fun testEquals() {
        val params = mapOf("customer" to "cus_123")

        assertEquals(
            FACTORY.createPost(
                PAYMENT_METHODS_URL,
                OPTIONS,
                params
            ),
            FACTORY.createPost(
                PAYMENT_METHODS_URL,
                OPTIONS,
                params
            )
        )

        assertNotEquals(
            FACTORY.createPost(
                PAYMENT_METHODS_URL,
                OPTIONS,
                params
            ),
            FACTORY.createPost(
                PAYMENT_METHODS_URL,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, "acct"),
                params
            )
        )
    }

    @Test
    fun getIncludesQueryParametersInUrl() {
        val url = FACTORY.createGet(
            SOURCES_URL,
            OPTIONS,
            mapOf("param" to "123")
        ).url
        assertEquals(url, "sources?param=123")
    }

    @Test
    fun deleteIncludesQueryParametersInUrl() {
        val url = FACTORY.createDelete(
            SOURCES_URL,
            OPTIONS,
            mapOf("param" to "123")
        ).url
        assertEquals(url, "sources?param=123")
    }

    private companion object {
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        private val FACTORY = ApiRequest.Factory()

        private const val SOURCES_URL = "sources"

        private const val PAYMENT_METHODS_URL = "payment_methods"
    }
}
