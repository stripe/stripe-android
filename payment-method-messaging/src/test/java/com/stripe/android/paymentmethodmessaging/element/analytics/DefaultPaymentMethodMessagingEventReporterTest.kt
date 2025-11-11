@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element.analytics

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultPaymentMethodMessagingEventReporterTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    private val durationProvider = FakeDurationProvider()
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val analyticsRequestExecutor = FakeAnalyticsRequestExecutor()
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

    @AfterTest
    fun tearDown() {
        analyticsRequestExecutor.validate()
        durationProvider.validate()
    }

    @Test
    fun `onInit should fire analytics request with expected value`() = runTest {
        eventReporter.onInit()
        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params["event"]).isEqualTo(PMME_INIT)
    }

    @Test
    fun `onLoadStarted should fire analytics request with expected value`() = runTest {
        eventReporter.simulateLoadStarted()
        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params["event"]).isEqualTo(PMME_LOAD_STARTED)
        assertThat(request.params["payment_methods"]).isEqualTo("affirm,klarna")
        assertThat(request.params["amount"]).isEqualTo(5000)
        assertThat(request.params["currency"]).isEqualTo("usd")
        assertThat(request.params["locale"]).isEqualTo("en")
        assertThat(request.params["country_code"]).isEqualTo("US")
        validateDurationStartCall()
    }

    @Test
    fun `onLoadSucceeded should fire analytics request with expected value`() = runTest {
        eventReporter.simulateLoadSucceeded()
        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params["event"]).isEqualTo(PMME_LOAD_SUCCEEDED)
        assertThat(request.params["payment_methods"]).isEqualTo("affirm,klarna")
        assertThat(request.params["content_type"]).isEqualTo("multi_partner")
        validateDurationEndCall()
    }

    @Test
    fun `onLoadFailed should fire analytics request with expected value`() = runTest {
        eventReporter.onLoadFailed(Throwable("something went wrong"))
        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params["event"]).isEqualTo(PMME_LOAD_FAILED)
        assertThat(request.params["error_message"]).isEqualTo("something went wrong")
        validateDurationEndCall()
    }

    @Test
    fun `onElementDisplayed should fire analytics request with expected value`() = runTest {
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
        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params["event"]).isEqualTo(PMME_DISPLAYED)
        assertThat(request.params["appearance"]).isEqualTo(
            mapOf(
                "font" to true,
                "style" to true,
                "text_color" to true,
                "info_icon_color" to true
            )
        )
    }

    @Test
    fun `onElementTapped should fire analytics request with expected value`() = runTest {
        eventReporter.onElementTapped()
        val request = analyticsRequestExecutor.requestTurbine.awaitItem()
        assertThat(request.params["event"]).isEqualTo(PMME_TAPPED)
    }

    @Test
    fun `on completed loading, should reset load timer`() = runTest {
        eventReporter.simulateLoadStarted()
        analyticsRequestExecutor.requestTurbine.awaitItem()
        validateDurationStartCall()
        eventReporter.simulateLoadSucceeded()
        analyticsRequestExecutor.requestTurbine.awaitItem()
        validateDurationEndCall()
    }

    @Test
    fun `on loading failed, should reset load timer`() = runTest {
        eventReporter.simulateLoadStarted()
        analyticsRequestExecutor.requestTurbine.awaitItem()
        validateDurationStartCall()
        eventReporter.onLoadFailed(Throwable())
        analyticsRequestExecutor.requestTurbine.awaitItem()
        validateDurationEndCall()
    }

    suspend fun validateDurationStartCall() {
        val duration = durationProvider.callsTurbine.awaitItem()
        assertThat(duration).isInstanceOf(FakeDurationProvider.Call.Start::class.java)
        assertThat(duration.key).isEqualTo(DurationProvider.Key.Loading)
        assertThat((duration as FakeDurationProvider.Call.Start).reset).isEqualTo(true)
    }

    suspend fun validateDurationEndCall() {
        val duration = durationProvider.callsTurbine.awaitItem()
        assertThat(duration).isInstanceOf(FakeDurationProvider.Call.End::class.java)
        assertThat(duration.key).isEqualTo(DurationProvider.Key.Loading)
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
