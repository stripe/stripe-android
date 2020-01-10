package com.stripe.android

import android.os.Build
import com.stripe.android.ApiRequest.Companion.HEADER_STRIPE_CLIENT_USER_AGENT
import com.stripe.android.model.CardFixtures
import java.util.Locale
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ApiRequestTest {

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun headers_withAllRequestOptions_properlyMapsRequestOptions() {
        Locale.setDefault(Locale.US)

        val stripeAccount = "acct_123abc"
        val headers = ApiRequest.createGet(
            StripeApiRepository.sourcesUrl,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, stripeAccount)
        ).headers

        assertEquals(
            "Bearer ${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}",
            headers["Authorization"]
        )
        assertEquals(ApiVersion.get().code, headers["Stripe-Version"])
        assertEquals(stripeAccount, headers["Stripe-Account"])
        assertEquals("en-US", headers["Accept-Language"])
    }

    @Test
    fun headers_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        val headers = ApiRequest.createGet(
            StripeApiRepository.sourcesUrl,
            OPTIONS
        ).headers

        assertTrue(headers.containsKey("Stripe-Version"))
        assertFalse(headers.containsKey("Stripe-Account"))
        assertTrue(headers.containsKey("Authorization"))
    }

    @Test
    fun headers_containsPropertyMapValues() {
        val headers = ApiRequest(
            StripeRequest.Method.GET,
            StripeApiRepository.sourcesUrl,
            params = null,
            options = OPTIONS,
            systemPropertySupplier = { UUID.randomUUID().toString() }
        ).headers

        val userAgentData = JSONObject(requireNotNull(headers[HEADER_STRIPE_CLIENT_USER_AGENT]))
        assertEquals(BuildConfig.VERSION_NAME, userAgentData.getString("bindings.version"))
        assertEquals("Java", userAgentData.getString("lang"))
        assertEquals("Stripe", userAgentData.getString("publisher"))
        assertEquals("android", userAgentData.getString("os.name"))
        assertEquals(Build.VERSION.SDK_INT, userAgentData.getString("os.version").toInt())
        assertTrue(userAgentData.getString("java.version").isNotBlank())
        assertTrue(userAgentData.getString("http.agent").isNotBlank())
    }

    @Test
    fun headers_correctlyAddsExpectedAdditionalParameters() {
        val headers = ApiRequest.createGet(
            StripeApiRepository.sourcesUrl,
            OPTIONS
        )
            .headers

        val expectedUserAgent = "Stripe/v1 AndroidBindings/${BuildConfig.VERSION_NAME}"
        assertEquals(expectedUserAgent, headers["User-Agent"])
        assertEquals("application/json", headers["Accept"])
        assertEquals("UTF-8", headers["Accept-Charset"])
    }

    @Test
    fun url_withCardData_createsProperQueryString() {
        val cardMap = NETWORK_UTILS.createCardTokenParams(CardFixtures.MINIMUM_CARD)
        val url = ApiRequest.createGet(
            StripeApiRepository.sourcesUrl,
            OPTIONS,
            cardMap
        ).url

        val expectedValue = "https://api.stripe.com/v1/sources?muid=BF3BF4D775100923AAAFA82884FB759001162E28&product_usage=&guid=6367C48DD193D56EA7B0BAAD25B19455E529F5EE&card%5Bexp_month%5D=1&card%5Bexp_year%5D=2050&card%5Bnumber%5D=4242424242424242&card%5Bcvc%5D=123"
        assertEquals(expectedValue, url)
    }

    @Test
    fun getContentType() {
        val contentType = ApiRequest.createGet(
            StripeApiRepository.sourcesUrl,
            OPTIONS
        ).contentType
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", contentType)
    }

    @Test
    fun body_withEmptyBody_shouldHaveZeroLength() {
        val bodyBytes = ApiRequest.createPost(
            StripeApiRepository.paymentMethodsUrl,
            OPTIONS
        ).bodyBytes
        assertTrue(bodyBytes.isEmpty())
    }

    @Test
    fun bodyBytes_withNonEmptyBody_shouldHaveNonZeroLength() {
        val params = mapOf("customer" to "cus_123")

        val bodyBytes =
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                params
            ).bodyBytes
        assertEquals(16, bodyBytes.size)
    }

    @Test
    fun testEquals() {
        val params = mapOf("customer" to "cus_123")

        assertEquals(
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                params
            ),
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                params
            )
        )

        assertNotEquals(
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS,
                params
            ),
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, "acct"),
                params
            )
        )
    }

    @Test
    fun headers_withAppInfo() {
        val apiRequest = ApiRequest.createGet(
            StripeApiRepository.paymentMethodsUrl,
            OPTIONS,
            appInfo = AppInfoTest.APP_INFO
        )
        val headers = apiRequest.headers
        assertEquals(
            "${StripeRequest.DEFAULT_USER_AGENT} MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
            headers["User-Agent"]
        )

        val stripeClientUserAgent = headers[HEADER_STRIPE_CLIENT_USER_AGENT]
            ?: error("Invalid JSON in `$HEADER_STRIPE_CLIENT_USER_AGENT`")
        val stripeClientUserAgentData = JSONObject(stripeClientUserAgent)
        assertEquals(
            JSONObject(
                """
                {
                    "name": "MyAwesomePlugin",
                    "version": "1.2.34",
                    "url": "https:\/\/myawesomeplugin.info",
                    "partner_id": "pp_partner_1234"
                }
                """.trimIndent()
            ).toString(),
            JSONObject(stripeClientUserAgentData.getString("application")).toString()
        )
    }

    @Test
    fun testGetLanguageTag() {
        Locale.setDefault(Locale.JAPAN)
        assertEquals("ja-JP", createGetRequest().createHeaders()["Accept-Language"])

        Locale.setDefault(Locale.ENGLISH)
        assertEquals("en", createGetRequest().createHeaders()["Accept-Language"])

        Locale.setDefault(Locale.ROOT)
        assertNull(createGetRequest().createHeaders()["Accept-Language"])
    }

    private companion object {
        private fun createGetRequest(): ApiRequest {
            return ApiRequest.createGet(
                StripeApiRepository.paymentMethodsUrl,
                OPTIONS
            )
        }

        private val NETWORK_UTILS = StripeNetworkUtils(
            UidParamsFactory(
                "com.example.app",
                FakeUidSupplier("abc123")
            )
        )

        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }
}
