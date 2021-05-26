package com.stripe.android.networking

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiVersion
import com.stripe.android.AppInfo
import com.stripe.android.AppInfoFixtures
import com.stripe.android.Stripe
import com.stripe.android.networking.StripeClientUserAgentHeaderFactory.Companion.HEADER_STRIPE_CLIENT_USER_AGENT
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.UUID
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ApiRequestHeadersFactoryTest {

    @Test
    fun create_shouldIncludeExpectedAcceptLanguageHeader() {
        assertThat(createHeaders(Locale.JAPAN)["Accept-Language"])
            .isEqualTo("ja-JP")

        assertThat(createHeaders(Locale.ENGLISH)["Accept-Language"]).isEqualTo("en")

        assertThat(createHeaders(Locale.ROOT)["Accept-Language"])
            .isNull()
    }

    @Test
    fun headers_withAllRequestOptions_properlyMapsRequestOptions() {
        val stripeAccount = "acct_123abc"
        val headers = createHeaders(
            locale = Locale.US,
            options = ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, stripeAccount)
        )

        assertThat(headers["Authorization"])
            .isEqualTo("Bearer ${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}")
        assertThat(headers["Stripe-Version"]).isEqualTo(ApiVersion.get().code)
        assertThat(headers["Stripe-Account"]).isEqualTo(stripeAccount)
        assertThat(headers["Accept-Language"]).isEqualTo("en-US")
    }

    @Test
    fun headers_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        val headers = createHeaders()

        assertThat(headers.containsKey("Stripe-Version")).isTrue()
        assertThat(headers.containsKey("Stripe-Account")).isFalse()
        assertThat(headers.containsKey("Authorization")).isTrue()
    }

    @Test
    fun headers_containsPropertyMapValues() {
        val headers = createHeaders()

        val userAgentData = JSONObject(
            requireNotNull(headers[HEADER_STRIPE_CLIENT_USER_AGENT])
        )
        assertThat(userAgentData.getString("bindings.version"))
            .isEqualTo(Stripe.VERSION_NAME)
        assertThat(userAgentData.getString("lang"))
            .isEqualTo("Java")
        assertThat(userAgentData.getString("publisher"))
            .isEqualTo("Stripe")
        assertThat(userAgentData.getString("os.name"))
            .isEqualTo("android")
        assertThat(userAgentData.getString("os.version").toInt())
            .isEqualTo(Build.VERSION.SDK_INT)
        assertThat(userAgentData.getString("http.agent"))
            .isNotEmpty()
    }

    @Test
    fun headers_correctlyAddsExpectedAdditionalParameters() {
        val headers = createHeaders()

        val expectedUserAgent = "Stripe/v1 AndroidBindings/${Stripe.VERSION_NAME}"
        assertThat(headers["User-Agent"]).isEqualTo(expectedUserAgent)
        assertThat(headers["Accept"]).isEqualTo("application/json")
        assertThat(headers["Accept-Charset"]).isEqualTo("UTF-8")
    }

    @Test
    fun headers_withAppInfo() {
        val headers = createHeaders(appInfo = AppInfoFixtures.DEFAULT)
        assertThat(headers["User-Agent"])
            .isEqualTo("${RequestHeadersFactory.getUserAgent()} MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)")

        val stripeClientUserAgent = headers[HEADER_STRIPE_CLIENT_USER_AGENT]
            ?: error("Invalid JSON in `$HEADER_STRIPE_CLIENT_USER_AGENT`")
        val stripeClientUserAgentData = JSONObject(stripeClientUserAgent)
        assertThat(JSONObject(stripeClientUserAgentData.getString("application")).toString())
            .isEqualTo(
                JSONObject(
                    """
            {
                "name": "MyAwesomePlugin",
                "version": "1.2.34",
                "url": "https:\/\/myawesomeplugin.info",
                "partner_id": "pp_partner_1234"
            }
                    """.trimIndent()
                ).toString()
            )
    }

    @Test
    fun `FraudDetection#create() should return expected map`() {
        val guid = UUID.randomUUID().toString()
        val headers = RequestHeadersFactory.FraudDetection(guid).create()
        assertThat(
            headers
        ).containsKey("User-Agent")
        assertThat(headers["Cookie"])
            .isEqualTo("m=$guid")
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
