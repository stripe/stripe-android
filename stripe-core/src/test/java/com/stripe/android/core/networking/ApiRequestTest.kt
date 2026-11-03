package com.stripe.android.core.networking

import android.os.Parcel
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.ApiKeyFixtures
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
    fun optionsParcelRoundTripPreservesCredentialsAndIdempotency() {
        val options = ApiConfiguration.State(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, "acct")
            .toRequestOptions(idempotencyKey = "request-id")
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(options, 0)
            parcel.setDataPosition(0)

            @Suppress("DEPRECATION")
            val restored = parcel.readParcelable<ApiRequest.Options>(ApiRequest.Options::class.java.classLoader)

            assertEquals(options, restored)
        } finally {
            parcel.recycle()
        }
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
                ApiConfiguration.State(
                    publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                    stripeAccountId = "acct",
                ).toRequestOptions(idempotencyKey = null),
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
        val options = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = null,
        ).toRequestOptions(idempotencyKey = null)
        val str = options.toString()
        assertFalse(str.contains(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
        assertFalse(str.contains("pk_test"))
    }

    private companion object {
        private val OPTIONS = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = null,
        ).toRequestOptions(idempotencyKey = null)

        private val FACTORY = ApiRequest.Factory()

        private const val SOURCES_URL = "sources"

        private const val PAYMENT_METHODS_URL = "payment_methods"
    }
}
