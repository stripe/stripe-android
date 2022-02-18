package com.stripe.android.networking

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ApiVersion
import com.stripe.android.AppInfo
import com.stripe.android.AppInfoFixtures
import com.stripe.android.Stripe
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_ACCEPT
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_ACCEPT_CHARSET
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_ACCEPT_LANGUAGE
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_AUTHORIZATION
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_CONTENT_TYPE
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_STRIPE_ACCOUNT
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_STRIPE_VERSION
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_USER_AGENT
import com.stripe.android.networking.RequestHeadersFactory.FraudDetection.Companion.HEADER_COOKIE
import com.stripe.android.networking.StripeClientUserAgentHeaderFactory.Companion.HEADER_STRIPE_CLIENT_USER_AGENT
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.UUID
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class RequestHeadersFactoriesTest {

    // Tests for RequestHeadersFactory.AbstractPaymentApiHeadersFactory
    @Test
    fun create_shouldIncludeExpectedAcceptLanguageHeader() {
        assertThat(createBasePaymentApiHeaders(Locale.JAPAN)[HEADER_ACCEPT_LANGUAGE])
            .isEqualTo("ja-JP")

        assertThat(createBasePaymentApiHeaders(Locale.ENGLISH)[HEADER_ACCEPT_LANGUAGE]).isEqualTo("en")

        assertThat(createBasePaymentApiHeaders(Locale.ROOT)[HEADER_ACCEPT_LANGUAGE])
            .isNull()
    }

    @Test
    fun headers_withAllRequestOptions_properlyMapsRequestOptions() {
        val stripeAccount = "acct_123abc"
        val headers = createBasePaymentApiHeaders(
            locale = Locale.US,
            options = ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, stripeAccount)
        )

        assertThat(headers[HEADER_AUTHORIZATION])
            .isEqualTo("Bearer ${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}")
        assertThat(headers[HEADER_STRIPE_VERSION]).isEqualTo(ApiVersion.get().code)
        assertThat(headers[HEADER_STRIPE_ACCOUNT]).isEqualTo(stripeAccount)
        assertThat(headers[HEADER_ACCEPT_LANGUAGE]).isEqualTo("en-US")
    }

    @Test
    fun headers_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        val headers = createBasePaymentApiHeaders()

        assertThat(headers.containsKey(HEADER_STRIPE_VERSION)).isTrue()
        assertThat(headers.containsKey(HEADER_STRIPE_ACCOUNT)).isFalse()
        assertThat(headers.containsKey(HEADER_AUTHORIZATION)).isTrue()
    }

    @Test
    fun headers_containsPropertyMapValues() {
        val headers = createBasePaymentApiHeaders()

        val userAgentData = JSONObject(
            requireNotNull(headers[HEADER_STRIPE_CLIENT_USER_AGENT])
        )
        assertThat(userAgentData.getString("bindings.version"))
            .isEqualTo(StripeSdkVersion.VERSION_NAME)
        assertThat(userAgentData.getString("lang"))
            .isEqualTo("Java")
        assertThat(userAgentData.getString("publisher"))
            .isEqualTo("Stripe")
        assertThat(userAgentData.getString("os.name"))
            .isEqualTo("android")
        assertThat(userAgentData.getString("os.version").toInt())
            .isEqualTo(Build.VERSION.SDK_INT)
    }

    @Test
    fun headers_correctlyAddsExpectedAdditionalParameters() {
        val headers = createBasePaymentApiHeaders()

        val expectedUserAgent = "Stripe/v1 AndroidBindings/${StripeSdkVersion.VERSION_NAME}"
        assertThat(headers[HEADER_USER_AGENT]).isEqualTo(expectedUserAgent)
        assertThat(headers[HEADER_ACCEPT]).isEqualTo("application/json")
        assertThat(headers[HEADER_ACCEPT_CHARSET]).isEqualTo("UTF-8")
    }

    @Test
    fun headers_withAppInfo() {
        val headers = createBasePaymentApiHeaders(appInfo = AppInfoFixtures.DEFAULT)
        assertThat(headers[HEADER_USER_AGENT])
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

    private fun createBasePaymentApiHeaders(
        locale: Locale = Locale.getDefault(),
        options: ApiRequest.Options = OPTIONS,
        appInfo: AppInfo? = null
    ): Map<String, String> {
        return RequestHeadersFactory.BasePaymentApiHeadersFactory(
            options = options,
            appInfo = appInfo,
            locale = locale
        ).create()
    }

    // Tests for RequestHeadersFactory.Api
    @Test
    fun api_post_headers() {
        val postHeaders = RequestHeadersFactory.Api(
            options = OPTIONS,
            appInfo = AppInfoFixtures.DEFAULT
        ).createPostHeader()
        assertThat(postHeaders[HEADER_CONTENT_TYPE])
            .isEqualTo("${StripeRequest.MimeType.Form}; charset=${RequestHeadersFactory.CHARSET}")
    }

    // Tests for RequestHeadersFactory.FileUpload
    @Test
    fun file_upload_post_headers() {
        val postHeaders = RequestHeadersFactory.FileUpload(
            options = OPTIONS,
            boundary = BOUNDARY
        ).createPostHeader()
        assertThat(postHeaders[HEADER_CONTENT_TYPE])
            .isEqualTo("${StripeRequest.MimeType.MultipartForm.code}; boundary=$BOUNDARY")
    }

    // Tests for RequestHeadersFactory.FraudDetection
    @Test
    fun fraud_detection_headers() {
        val guid = UUID.randomUUID().toString()
        val headers = RequestHeadersFactory.FraudDetection(guid).create()
        assertThat(
            headers
        ).containsKey(HEADER_USER_AGENT)
        assertThat(headers[HEADER_COOKIE])
            .isEqualTo("m=$guid")
    }

    @Test
    fun fraud_detection_post_headers() {
        val postHeaders =
            RequestHeadersFactory.FraudDetection(UUID.randomUUID().toString()).createPostHeader()
        assertThat(postHeaders[HEADER_CONTENT_TYPE])
            .isEqualTo("${StripeRequest.MimeType.Json}; charset=${RequestHeadersFactory.CHARSET}")
    }

    // Tests for RequestHeadersFactory.Analytics
    @Test
    fun analytics_headers() {
        val headers = RequestHeadersFactory.Analytics.create()
        assertThat(headers).containsKey(HEADER_USER_AGENT)
        assertThat(headers).containsKey(HEADER_ACCEPT_CHARSET)
    }

    @Test
    fun analytics_post_headers() {
        assertThat(
            RequestHeadersFactory.Analytics.createPostHeader()
        ).isEmpty()
    }

    private companion object {
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        private const val BOUNDARY = "TEST_BOUNDARY"
    }
}
