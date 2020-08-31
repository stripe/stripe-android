package com.stripe.android

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardParamsFixtures
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
    fun url_withCardData_createsProperQueryString() {
        val url = FACTORY.createGet(
            url = StripeApiRepository.sourcesUrl,
            options = OPTIONS,
            params = CardParamsFixtures.MINIMUM.toParamMap()
                .plus(FINGERPRINT_DATA.params)
        ).url

        assertThat(Uri.parse(url))
            .isEqualTo(Uri.parse("https://api.stripe.com/v1/sources?muid=${FINGERPRINT_DATA.muid}&guid=${FINGERPRINT_DATA.guid}&card%5Bnumber%5D=4242424242424242&card%5Bexp_month%5D=1&card%5Bcvc%5D=123&card%5Bexp_year%5D=2050&sid=${FINGERPRINT_DATA.sid}"))
    }

    @Test
    fun getContentType() {
        val contentType = FACTORY.createGet(
            StripeApiRepository.sourcesUrl,
            OPTIONS
        ).contentType
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", contentType)
    }

    @Test
    fun writeBody_withEmptyBody_shouldHaveZeroLength() {
        ByteArrayOutputStream().use {
            FACTORY.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS
            ).writeBody(it)
            assertTrue(it.size() == 0)
        }
    }

    @Test
    fun writeBody_withNonEmptyBody_shouldHaveNonZeroLength() {
        ByteArrayOutputStream().use {
            FACTORY.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                mapOf("customer" to "cus_123")
            ).writeBody(it)
            assertEquals(16, it.size())
        }
    }

    @Test
    fun testEquals() {
        val params = mapOf("customer" to "cus_123")

        assertEquals(
            FACTORY.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                params
            ),
            FACTORY.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                params
            )
        )

        assertNotEquals(
            FACTORY.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                params
            ),
            FACTORY.createPost(
                StripeApiRepository.paymentMethodsUrl,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, "acct"),
                params
            )
        )
    }

    private companion object {
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        private val FACTORY = ApiRequest.Factory()

        private val FINGERPRINT_DATA = FingerprintDataFixtures.create()
    }
}
