package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiKeyFixtures
import com.stripe.android.core.ApiVersion
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class ApiRequestTest {
    @Test
    fun `GET requests use Dahlia`() {
        val request = FACTORY.createGet(SOURCES_URL, OPTIONS)

        assertThat(request.headers[HEADER_STRIPE_VERSION]).isEqualTo("2026-08-26.dahlia")
    }

    @Test
    fun `POST requests use Dahlia`() {
        val request = FACTORY.createPost(PAYMENT_METHODS_URL, OPTIONS)

        assertThat(request.headers[HEADER_STRIPE_VERSION]).isEqualTo("2026-08-26.dahlia")
    }

    @Test
    fun `DELETE requests use Dahlia`() {
        val request = FACTORY.createDelete(SOURCES_URL, OPTIONS)

        assertThat(request.headers[HEADER_STRIPE_VERSION]).isEqualTo("2026-08-26.dahlia")
    }

    @Test
    fun `requests preserve beta suffixes on Dahlia`() {
        val factory = ApiRequest.Factory(
            apiVersion = ApiVersion(setOf("financial_connections_client_api_beta=v1", "another_beta=v2")).code
        )
        val request = factory.createGet(SOURCES_URL, OPTIONS)

        assertThat(request.headers[HEADER_STRIPE_VERSION])
            .isEqualTo("2026-08-26.dahlia;financial_connections_client_api_beta=v1;another_beta=v2")
    }

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

    @Test
    fun optionsToStringDoesNotExposeApiKey() {
        val options = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        val str = options.toString()
        assertFalse(str.contains(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
        assertFalse(str.contains("pk_test"))
    }

    private companion object {
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        private val FACTORY = ApiRequest.Factory()

        private const val SOURCES_URL = "sources"

        private const val PAYMENT_METHODS_URL = "payment_methods"
    }
}
