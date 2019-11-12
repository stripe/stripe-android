package com.stripe.android

import android.os.Build
import com.stripe.android.ApiRequest.Companion.HEADER_STRIPE_CLIENT_USER_AGENT
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.CardFixtures
import java.io.UnsupportedEncodingException
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONException
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
    fun getHeaders_withAllRequestOptions_properlyMapsRequestOptions() {
        Locale.setDefault(Locale.US)

        val stripeAccount = "acct_123abc"
        val headers = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccount))
            .headers

        assertEquals(
            "Bearer ${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}",
            headers["Authorization"]
        )
        assertEquals(ApiVersion.get().code, headers["Stripe-Version"])
        assertEquals(stripeAccount, headers["Stripe-Account"])
        assertEquals("en-US", headers["Accept-Language"])
    }

    @Test
    fun getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        val headerMap = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
            .headers

        assertTrue(headerMap.containsKey("Stripe-Version"))
        assertFalse(headerMap.containsKey("Stripe-Account"))
        assertTrue(headerMap.containsKey("Authorization"))
    }

    @Test
    @Throws(JSONException::class)
    fun getHeaders_containsPropertyMapValues() {
        val headers = ApiRequest(
            StripeRequest.Method.GET,
            StripeApiRepository.sourcesUrl,
            params = null,
            options = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            systemPropertySupplier = FakeSystemPropertySupplier()
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
    fun getHeaders_correctlyAddsExpectedAdditionalParameters() {
        val headerMap = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
            .headers

        val expectedUserAgent = "Stripe/v1 AndroidBindings/${BuildConfig.VERSION_NAME}"
        assertEquals(expectedUserAgent, headerMap[StripeRequest.HEADER_USER_AGENT])
        assertEquals("application/json", headerMap["Accept"])
        assertEquals("UTF-8", headerMap["Accept-Charset"])
    }

    @Test
    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    fun createQuery_withCardData_createsProperQueryString() {
        val cardMap = NETWORK_UTILS.createCardTokenParams(CardFixtures.MINIMUM_CARD)
        val query = ApiRequest.createGet(
            StripeApiRepository.sourcesUrl,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            cardMap
        ).query

        val expectedValue = "muid=BF3BF4D775100923AAAFA82884FB759001162E28&product_usage=&guid=6367C48DD193D56EA7B0BAAD25B19455E529F5EE&card%5Bexp_month%5D=1&card%5Bexp_year%5D=2050&card%5Bnumber%5D=4242424242424242&card%5Bcvc%5D=123"
        assertEquals(expectedValue, query)
    }

    @Test
    fun getContentType() {
        val contentType = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
            .contentType
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", contentType)
    }

    @Test
    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    fun getOutputBytes_withEmptyBody_shouldHaveZeroLength() {
        val output = ApiRequest.createPost(StripeApiRepository.paymentMethodsUrl,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY))
            .getOutputBytes()
        assertEquals(0, output.size)
    }

    @Test
    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    fun getOutputBytes_withNonEmptyBody_shouldHaveNonZeroLength() {
        val params = mapOf("customer" to "cus_123")

        val output = ApiRequest.createPost(
            StripeApiRepository.paymentMethodsUrl,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            params
        ).getOutputBytes()
        assertEquals(16, output.size)
    }

    @Test
    fun testEquals() {
        val params = mapOf("customer" to "cus_123")
        assertEquals(
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
                params
            ),
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
                params
            )
        )

        assertNotEquals(
            ApiRequest.createPost(
                StripeApiRepository.paymentMethodsUrl,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
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
    @Throws(JSONException::class)
    fun getHeaders_withAppInfo() {
        val apiRequest = ApiRequest.createGet(
            StripeApiRepository.paymentMethodsUrl,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            appInfo = AppInfoTest.APP_INFO
        )
        val headers = apiRequest.headers
        assertEquals(
            "${StripeRequest.DEFAULT_USER_AGENT} MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
            headers[StripeRequest.HEADER_USER_AGENT]
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
        assertEquals("ja-JP", createGetRequest().languageTag)

        Locale.setDefault(Locale.ENGLISH)
        assertEquals("en", createGetRequest().languageTag)

        Locale.setDefault(Locale.ROOT)
        assertNull(createGetRequest().languageTag)
    }

    private companion object {
        private fun createGetRequest(): ApiRequest {
            return ApiRequest.createGet(
                StripeApiRepository.paymentMethodsUrl,
                ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
            )
        }

        private val NETWORK_UTILS = StripeNetworkUtils(
            UidParamsFactory(
                "com.example.app",
                FakeUidSupplier("abc123")
            )
        )
    }
}
