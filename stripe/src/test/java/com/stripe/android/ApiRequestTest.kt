package com.stripe.android

import com.stripe.android.model.CardFixtures
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ApiRequestTest {

    @Test
    fun url_withCardData_createsProperQueryString() {
        val cardMap = CardFixtures.MINIMUM_CARD.toParamMap()
            .plus(NETWORK_UTILS.createUidParams())
        val url = FACTORY.createGet(
            StripeApiRepository.sourcesUrl,
            OPTIONS,
            cardMap
        ).url

        val expectedValue = "https://api.stripe.com/v1/sources?muid=BF3BF4D775100923AAAFA82884FB759001162E28&guid=6367C48DD193D56EA7B0BAAD25B19455E529F5EE&card%5Bexp_month%5D=1&card%5Bexp_year%5D=2050&card%5Bnumber%5D=4242424242424242&card%5Bcvc%5D=123"
        assertEquals(expectedValue, url)
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
        private val NETWORK_UTILS = StripeNetworkUtils(
            UidParamsFactory(
                "com.example.app",
                FakeUidSupplier("abc123")
            )
        )

        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        private val FACTORY = ApiRequest.Factory()
    }
}
