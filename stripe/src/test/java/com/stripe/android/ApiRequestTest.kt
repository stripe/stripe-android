package com.stripe.android

import android.os.Build
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
        val stripeAccount = "acct_123abc"
        val headers = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options.create(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccount), null)
            .headers

        assertEquals(
            "Bearer ${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}",
            headers["Authorization"]
        )
        assertEquals(ApiVersion.get().code, headers["Stripe-Version"])
        assertEquals(stripeAccount, headers["Stripe-Account"])
        assertFalse(headers.contains("Accept-Language"))
    }

    @Test
    fun getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        val headerMap = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
            .headers

        assertTrue(headerMap.containsKey("Stripe-Version"))
        assertFalse(headerMap.containsKey("Stripe-Account"))
        assertTrue(headerMap.containsKey("Authorization"))
    }

    @Test
    @Throws(JSONException::class)
    fun getHeaders_containsPropertyMapValues() {
        val headers = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
            .headers

        val userAgentData = JSONObject(headers["X-Stripe-Client-User-Agent"]!!)
        assertEquals(BuildConfig.VERSION_NAME, userAgentData.getString("bindings.version"))
        assertEquals("Java", userAgentData.getString("lang"))
        assertEquals("Stripe", userAgentData.getString("publisher"))
        assertEquals("android", userAgentData.getString("os.name"))
        assertEquals(Build.VERSION.SDK_INT,
            Integer.parseInt(userAgentData.getString("os.version")))
        assertTrue(userAgentData.getString("java.version").startsWith("1.8.0"))
    }

    @Test
    fun getHeaders_correctlyAddsExpectedAdditionalParameters() {
        val headerMap = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
            .headers

        val expectedUserAgent = String.format(Locale.ROOT, "Stripe/v1 AndroidBindings/%s",
            BuildConfig.VERSION_NAME)
        assertEquals(expectedUserAgent, headerMap[StripeRequest.HEADER_USER_AGENT])
        assertEquals("application/json", headerMap["Accept"])
        assertEquals("UTF-8", headerMap["Accept-Charset"])
    }

    @Test
    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    fun createQuery_withCardData_createsProperQueryString() {
        val cardMap = NETWORK_UTILS.createCardTokenParams(CardFixtures.MINIMUM_CARD)
        val query = ApiRequest.createGet(StripeApiRepository.sourcesUrl, cardMap,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
            .createQuery()

        val expectedValue = "muid=BF3BF4D775100923AAAFA82884FB759001162E28&product_usage=&guid=6367C48DD193D56EA7B0BAAD25B19455E529F5EE&card%5Bexp_month%5D=1&card%5Bexp_year%5D=2050&card%5Bnumber%5D=4242424242424242&card%5Bcvc%5D=123"
        assertEquals(expectedValue, query)
    }

    @Test
    fun getContentType() {
        val contentType = ApiRequest.createGet(StripeApiRepository.sourcesUrl,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
            .contentType
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", contentType)
    }

    @Test
    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    fun getOutputBytes_withEmptyBody_shouldHaveZeroLength() {
        val output = ApiRequest.createPost(StripeApiRepository.paymentMethodsUrl,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
            .getOutputBytes()
        assertEquals(0, output.size)
    }

    @Test
    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    fun getOutputBytes_withNonEmptyBody_shouldHaveNonZeroLength() {
        val params = mapOf("customer" to "cus_123")

        val output = ApiRequest.createPost(StripeApiRepository.paymentMethodsUrl,
            params,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
            .getOutputBytes()
        assertEquals(16, output.size)
    }

    @Test
    fun testEquals() {
        val params = mapOf("customer" to "cus_123")
        assertEquals(
            ApiRequest.createPost(StripeApiRepository.paymentMethodsUrl,
                params,
                ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null),
            ApiRequest.createPost(StripeApiRepository.paymentMethodsUrl,
                params,
                ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null)
        )

        assertNotEquals(
            ApiRequest.createPost(StripeApiRepository.paymentMethodsUrl,
                params,
                ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY), null),
            ApiRequest.createPost(StripeApiRepository.paymentMethodsUrl,
                params,
                ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                    "acct"), null)
        )
    }

    @Test
    @Throws(JSONException::class)
    fun getHeaders_withAppInfo() {
        val apiRequest = ApiRequest.createGet(
            StripeApiRepository.paymentMethodsUrl,
            ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            AppInfoTest.APP_INFO)
        val headers = apiRequest.headers
        assertEquals(
            "${StripeRequest.DEFAULT_USER_AGENT} MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
            headers[StripeRequest.HEADER_USER_AGENT]
        )

        val userAgentData = JSONObject(
            headers["X-Stripe-Client-User-Agent"]
                ?: error("Invalid JSON in `X-Stripe-Client-User-Agent`")
        )
        assertEquals(
            JSONObject(
                """
                {
                    "name": "MyAwesomePlugin",
                    "partner_id": "pp_partner_1234",
                    "version": "1.2.34",
                    "url": "https:\/\/myawesomeplugin.info"
                }
                """.trimIndent()
            ).toString(),
            JSONObject(userAgentData.getString("application")).toString()
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

    companion object {
        private fun createGetRequest(): ApiRequest {
            return ApiRequest.createGet(
                StripeApiRepository.paymentMethodsUrl,
                ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
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
