@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element.analytics

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingContent
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview
import com.stripe.android.paymentmethodmessaging.element.analytics.PaymentMethodMessagingEvent.Companion.PMME_DISPLAYED
import com.stripe.android.paymentmethodmessaging.element.analytics.PaymentMethodMessagingEvent.Companion.PMME_INIT
import com.stripe.android.paymentmethodmessaging.element.analytics.PaymentMethodMessagingEvent.Companion.PMME_LOAD_FAILED
import com.stripe.android.paymentmethodmessaging.element.analytics.PaymentMethodMessagingEvent.Companion.PMME_LOAD_STARTED
import com.stripe.android.paymentmethodmessaging.element.analytics.PaymentMethodMessagingEvent.Companion.PMME_LOAD_SUCCEEDED
import com.stripe.android.paymentmethodmessaging.element.analytics.PaymentMethodMessagingEvent.Companion.PMME_TAPPED
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultPaymentMethodMessagingEventReporterTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    private val durationProvider = FakeDurationProvider()
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageInfo,
        publishableKeyProvider = { "pk_test_123" },
        networkTypeProvider = { "5G" },
    )
    private val eventReporter = DefaultPaymentMethodMessagingEventReporter(
        analyticsRequestExecutor = analyticsRequestExecutor,
        analyticsRequestFactory = analyticsRequestFactory,
        durationProvider = durationProvider,
        workContext = testDispatcher
    )

    @Test
    fun `onInit should fire analytics request with expected value`() {
        eventReporter.onInit()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == PMME_INIT
            }
        )
    }

    @Test
    fun `onLoadStarted should fire analytics request with expected value`() {
        eventReporter.simulateLoadStarted()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == PMME_LOAD_STARTED
                req.params["payment_methods"] == listOf("affirm", "klarna")
                req.params["amount"] == "5000"
                req.params["currency"] == "usd"
                req.params["locale"] == "en"
                req.params["country_code"] == "US"
            }
        )
    }

    @Test
    fun `onLoadSucceeded should fire analytics request with expected value`() {
        eventReporter.simulateLoadSucceeded()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == PMME_LOAD_SUCCEEDED
                req.params["payment_methods"] == listOf("affirm", "klarna")
                req.params["content_type"] == "multi_partner"
            }
        )
    }

    @Test
    fun `onLoadFailed should fire analytics request with expected value`() {
        eventReporter.onLoadFailed(Throwable("something went wrong"))
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == PMME_LOAD_FAILED
                req.params["error_message"] == "something went wrong"
            }
        )
    }

    @Test
    fun `onElementDisplayed should fire analytics request with expected value`() {
        eventReporter.onElementDisplayed(
            appearance = PaymentMethodMessagingElement.Appearance()
                .colors(
                    PaymentMethodMessagingElement.Appearance.Colors()
                        .infoIconColor(Color.Black.toArgb())
                        .textColor(Color.Blue.toArgb())
                )
                .font(
                    PaymentMethodMessagingElement.Appearance.Font()
                        .fontSizeSp(16f)
                )
                .theme(PaymentMethodMessagingElement.Appearance.Theme.DARK)
                .build()
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == PMME_DISPLAYED
                req.params["appearance"] == mapOf(
                    "font" to true,
                    "style" to true,
                    "text_color" to true,
                    "info_icon_color" to true
                )
            }
        )
    }

    @Test
    fun `onElementTapped should fire analytics request with expected value`() {
        eventReporter.onElementTapped()
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == PMME_TAPPED
            }
        )
    }

    @Test
    fun `on completed loading, should reset load timer`() {
        eventReporter.simulateLoadSucceeded()
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(
                    key = DurationProvider.Key.Loading,
                    reset = true
                )
            )
        )
        eventReporter.simulateLoadSucceeded()
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.End(
                    key = DurationProvider.Key.Loading,
                )
            )
        )
    }

    @Test
    fun `on loading failed, should reset load timer`() {
        eventReporter.simulateLoadSucceeded()
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(
                    key = DurationProvider.Key.Loading,
                    reset = true
                )
            )
        )
        eventReporter.onLoadFailed(Throwable())
        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.End(
                    key = DurationProvider.Key.Loading,
                )
            )
        )
    }

    private fun DefaultPaymentMethodMessagingEventReporter.simulateLoadStarted() {
        this.onLoadStarted(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(5000L)
                .currency("usd")
                .locale("en")
                .countryCode("US")
                .paymentMethodTypes(listOf(PaymentMethod.Type.Affirm, PaymentMethod.Type.Klarna))
                .build()
        )
    }

    private fun DefaultPaymentMethodMessagingEventReporter.simulateLoadSucceeded() {
        this.onLoadSucceeded(
            paymentMethods = listOf("affirm", "klarna"),
            content = PaymentMethodMessagingContent.get(
                PaymentMethodMessage.MultiPartner(
                    promotion = "",
                    lightImages = listOf(),
                    darkImages = listOf(),
                    flatImages = listOf(),
                    paymentMethods = listOf("affirm", "klarna"),
                    learnMore = PaymentMethodMessageLearnMore(
                        message = "",
                        url = ""
                    )
                )
            ) {},
        )
    }
}
