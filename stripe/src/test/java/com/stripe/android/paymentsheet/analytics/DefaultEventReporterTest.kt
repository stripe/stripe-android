package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AnalyticsDataFactory
import com.stripe.android.networking.AnalyticsRequest
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DefaultEventReporterTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = AnalyticsRequest.Factory()
    private val analyticsDataFactory = AnalyticsDataFactory(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private val sessionId = SessionId()

    private val eventReporterFactory: (EventReporter.Mode) -> EventReporter = { mode ->
        DefaultEventReporter(
            mode,
            sessionId,
            FakeDeviceIdRepository(),
            analyticsRequestExecutor,
            analyticsRequestFactory,
            analyticsDataFactory,
            testDispatcher
        )
    }

    private val completeEventReporter = eventReporterFactory(EventReporter.Mode.Complete)
    private val customEventReporter = eventReporterFactory(EventReporter.Mode.Custom)

    @Test
    fun `onInit() should fire analytics request with expected event value`() {
        completeEventReporter.onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.compactParams?.get("event") == "mc_complete_init_customer_googlepay"
            }
        )
    }

    @Test
    fun `onPaymentSuccess() should fire analytics request with expected event value`() {
        completeEventReporter.onPaymentSuccess(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.compactParams?.get("event") == "mc_complete_payment_savedpm_success"
            }
        )
    }

    @Test
    fun `onSelectPaymentOption() should fire analytics request with expected event value`() {
        customEventReporter.onSelectPaymentOption(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.compactParams?.get("event") == "mc_custom_paymentoption_savedpm_select"
            }
        )
    }

    @Test
    fun `onPaymentSuccess() should fire analytics request with session id`() {
        completeEventReporter.onPaymentSuccess(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.compactParams?.get("session_id") == sessionId.value
            }
        )
    }

    @Test
    fun `onPaymentSuccess() should fire analytics request with valid device id`() {
        completeEventReporter.onPaymentSuccess(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                val deviceIdValue = requireNotNull(req.compactParams?.get("device_id")).toString()
                UUID.fromString(deviceIdValue) != null
            }
        )
    }
}
