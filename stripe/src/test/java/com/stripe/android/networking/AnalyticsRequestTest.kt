package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.AppInfoFixtures
import com.stripe.android.Logger
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AnalyticsRequestTest {

    private val logger: Logger = mock()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val factory = AnalyticsRequest.Factory(logger)

    @Test
    fun factoryCreate_createsExpectedObject() {
        val sdkVersion = Stripe.VERSION_NAME
        val analyticsRequest = factory.create(
            params = AnalyticsDataFactory(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                .createPaymentMethodCreationParams(
                    "pm_12345",
                    PaymentMethod.Type.Card,
                    emptySet()
                ),
            options = ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            appInfo = AppInfoFixtures.DEFAULT
        )
        assertThat(analyticsRequest.headers)
            .isEqualTo(
                mapOf(
                    "Accept" to "application/json",
                    "X-Stripe-Client-User-Agent" to
                        """
                    {"os.name":"android","os.version":"28","bindings.version":"$sdkVersion","lang":"Java","publisher":"Stripe","http.agent":"","application":{"name":"MyAwesomePlugin","version":"1.2.34","url":"https:\/\/myawesomeplugin.info","partner_id":"pp_partner_1234"}}
                        """.trimIndent(),
                    "Stripe-Version" to "2020-03-02",
                    "Authorization" to "Bearer pk_test_123",
                    "Accept-Language" to "en-US",
                    "User-Agent" to "Stripe/v1 AndroidBindings/$sdkVersion MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
                    "Accept-Charset" to "UTF-8"
                )
            )
        val requestUrl = analyticsRequest.url

        assertThat(requestUrl)
            .isEqualTo("https://q.stripe.com?publishable_key=pk_test_123&app_version=0&bindings_version=$sdkVersion&os_version=28&os_release=9&device_type=unknown_Android_robolectric&source_type=card&app_name=com.stripe.android.test&payment_method_id=pm_12345&analytics_ua=analytics.stripe_android-1.0&os_name=REL&event=stripe_android.payment_method_creation")
    }

    @Test
    fun factoryCreate_shouldLogRequest() {
        factory.create(
            AnalyticsDataFactory(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
                .createPaymentMethodCreationParams(
                    "pm_12345",
                    PaymentMethod.Type.Card,
                    emptySet()
                ),
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        )

        verify(logger).info(
            "Event: stripe_android.payment_method_creation"
        )
    }
}
