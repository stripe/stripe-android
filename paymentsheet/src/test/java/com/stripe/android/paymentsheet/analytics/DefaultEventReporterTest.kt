package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DefaultEventReporterTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = AnalyticsRequestFactory(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private val eventReporterFactory: (EventReporter.Mode) -> EventReporter = { mode ->
        DefaultEventReporter(
            mode,
            FakeDeviceIdRepository(),
            analyticsRequestExecutor,
            analyticsRequestFactory,
            testDispatcher
        )
    }

    private val completeEventReporter = eventReporterFactory(EventReporter.Mode.Complete)
    private val customEventReporter = eventReporterFactory(EventReporter.Mode.Custom)

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

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

    @Test
    fun `constructor does not read from PaymentConfiguration`() {
        PaymentConfiguration.clearInstance()
        // Would crash if it tries to read from the uninitialized PaymentConfiguration
        DefaultEventReporter(
            EventReporter.Mode.Complete,
            ApplicationProvider.getApplicationContext()
        )
    }
}
