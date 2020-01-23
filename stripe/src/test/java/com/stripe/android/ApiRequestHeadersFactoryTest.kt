package com.stripe.android

import android.os.Build
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiRequestHeadersFactoryTest {
    @Test
    fun create_shouldIncludeExpectedAcceptLanguageHeader() {
        assertEquals(
            "ja-JP",
            createHeaders(Locale.JAPAN)["Accept-Language"]
        )

        assertEquals(
            "en",
            createHeaders(Locale.ENGLISH)["Accept-Language"]
        )

        assertNull(
            createHeaders(Locale.ROOT)["Accept-Language"]
        )
    }

    @Test
    fun headers_withAllRequestOptions_properlyMapsRequestOptions() {
        val stripeAccount = "acct_123abc"
        val headers = createHeaders(
            locale = Locale.US,
            options = ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, stripeAccount)
        )

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
        val headers = createHeaders()

        assertTrue(headers.containsKey("Stripe-Version"))
        assertFalse(headers.containsKey("Stripe-Account"))
        assertTrue(headers.containsKey("Authorization"))
    }

    @Test
    fun headers_containsPropertyMapValues() {
        val headers = createHeaders()

        val userAgentData = JSONObject(requireNotNull(headers[ApiRequest.HEADER_STRIPE_CLIENT_USER_AGENT]))
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
        val headers = createHeaders()

        val expectedUserAgent = "Stripe/v1 AndroidBindings/${BuildConfig.VERSION_NAME}"
        assertEquals(expectedUserAgent, headers["User-Agent"])
        assertEquals("application/json", headers["Accept"])
        assertEquals("UTF-8", headers["Accept-Charset"])
    }

    @Test
    fun headers_withAppInfo() {
        val headers = createHeaders(appInfo = AppInfoTest.APP_INFO)
        assertEquals(
            "${RequestHeadersFactory.getUserAgent()} MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
            headers["User-Agent"]
        )

        val stripeClientUserAgent = headers[ApiRequest.HEADER_STRIPE_CLIENT_USER_AGENT]
            ?: error("Invalid JSON in `${ApiRequest.HEADER_STRIPE_CLIENT_USER_AGENT}`")
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

    private fun createHeaders(
        locale: Locale = Locale.getDefault(),
        options: ApiRequest.Options = OPTIONS,
        appInfo: AppInfo? = null
    ): Map<String, String> {
        return RequestHeadersFactory.Api(
            options = options,
            appInfo = appInfo,
            locale = locale,
            systemPropertySupplier = { UUID.randomUUID().toString() }
        ).create()
    }

    private companion object {
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }
}
