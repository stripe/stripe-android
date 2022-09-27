package com.stripe.android.core.networking

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiKeyFixtures
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.AppInfoFixtures
import com.stripe.android.core.networking.RequestHeadersFactory.Companion.KOTLIN
import com.stripe.android.core.networking.RequestHeadersFactory.Companion.LANG
import com.stripe.android.core.networking.RequestHeadersFactory.Companion.MODEL
import com.stripe.android.core.networking.RequestHeadersFactory.Companion.TYPE
import com.stripe.android.core.networking.RequestHeadersFactory.FraudDetection.Companion.HEADER_COOKIE
import com.stripe.android.core.networking.StripeClientUserAgentHeaderFactory.Companion.HEADER_STRIPE_CLIENT_USER_AGENT
import com.stripe.android.core.version.StripeSdkVersion
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
    fun headers_withInternalAppInfo() {
        val headers = createBasePaymentApiHeaders(appInfo = AppInfoFixtures.DEFAULT)
        assertThat(headers[HEADER_USER_AGENT])
            .isEqualTo("${RequestHeadersFactory.getUserAgent()} MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)")

        val xStripeUserAgent = JSONObject(
            requireNotNull(headers[HEADER_X_STRIPE_USER_AGENT])
        )
        assertThat(xStripeUserAgent[LANG]).isEqualTo(KOTLIN)
        assertThat(xStripeUserAgent[AnalyticsFields.BINDINGS_VERSION]).isEqualTo(StripeSdkVersion.VERSION_NAME)
        assertThat(xStripeUserAgent[AnalyticsFields.OS_VERSION]).isEqualTo("${Build.VERSION.SDK_INT}")
        assertThat(xStripeUserAgent.has(TYPE)).isTrue()
        assertThat(xStripeUserAgent[MODEL]).isEqualTo(Build.MODEL)
        assertThat(xStripeUserAgent["name"]).isEqualTo("MyAwesomePlugin")
        assertThat(xStripeUserAgent["version"]).isEqualTo("1.2.34")
        assertThat(xStripeUserAgent["url"]).isEqualTo("https://myawesomeplugin.info")
        assertThat(xStripeUserAgent["partner_id"]).isEqualTo("pp_partner_1234")

        val stripeClientUserAgent = headers[HEADER_STRIPE_CLIENT_USER_AGENT]
            ?: error("Invalid JSON in `$HEADER_STRIPE_CLIENT_USER_AGENT`")
        val stripeClientUserAgentData = JSONObject(stripeClientUserAgent)
        assertThat(stripeClientUserAgentData.get("application"))
            .isEqualTo("{name=MyAwesomePlugin, version=1.2.34, url=https://myawesomeplugin.info, partner_id=pp_partner_1234}")
    }

    @Test
    fun headers_withChineseSimplified_hasProperLanguageTag() {
        val stripeAccount = "acct_123abc"
        val headers = createBasePaymentApiHeaders(
            locale = Locale.SIMPLIFIED_CHINESE,
            options = ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, stripeAccount)
        )

        assertThat(headers[HEADER_AUTHORIZATION])
            .isEqualTo("Bearer ${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}")
        assertThat(headers[HEADER_STRIPE_VERSION]).isEqualTo(ApiVersion.get().code)
        assertThat(headers[HEADER_STRIPE_ACCOUNT]).isEqualTo(stripeAccount)
        assertThat(headers[HEADER_ACCEPT_LANGUAGE]).isEqualTo("zh-CN")
    }

    private fun createBasePaymentApiHeaders(
        locale: Locale = Locale.getDefault(),
        options: ApiRequest.Options = OPTIONS,
        appInfo: AppInfo? = null
    ): Map<String, String> {
        return RequestHeadersFactory.BaseApiHeadersFactory(
            optionsProvider = { options },
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
        private const val BOUNDARY = "TEST_BOUNDARY"
        private val OPTIONS = ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }
}
