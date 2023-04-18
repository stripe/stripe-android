package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultEventReporterTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val eventTimeProvider = mock<EventTimeProvider>().apply {
        whenever(currentTimeMillis()).thenReturn(1000L)
    }
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private val eventReporterFactory: (EventReporter.Mode) -> EventReporter = { mode ->
        DefaultEventReporter(
            mode,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            eventTimeProvider,
            testDispatcher
        )
    }

    private val completeEventReporter = eventReporterFactory(EventReporter.Mode.Complete)
    private val customEventReporter = eventReporterFactory(EventReporter.Mode.Custom)

    @Test
    fun `onInit() should fire analytics request with expected event value`() {
        completeEventReporter.onInit(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isDecoupling = false,
            isServerSideConfirmation = false,
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_init_customer_googlepay" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onShowExistingPaymentOptions() should fire analytics request with expected event value`() {
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = true,
            activeLinkSession = false,
            currency = "usd",
            isDecoupling = false,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_sheet_savedpm_show" &&
                    req.params["link_enabled"] == true &&
                    req.params["active_link_session"] == false &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onShowNewPaymentOptionForm() should fire analytics request with expected event value`() {
        completeEventReporter.onShowNewPaymentOptionForm(
            linkEnabled = false,
            activeLinkSession = true,
            currency = "usd",
            isDecoupling = false,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_sheet_newpm_show" &&
                    req.params["link_enabled"] == false &&
                    req.params["active_link_session"] == true &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onPaymentSuccess() should fire analytics request with expected event value`() {
        // Log initial event so that duration is tracked
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = false,
            activeLinkSession = false,
            currency = "usd",
            isDecoupling = false,
        )
        reset(analyticsRequestExecutor)
        whenever(eventTimeProvider.currentTimeMillis()).thenReturn(2000L)

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            currency = "usd",
            isDecoupling = false,
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_payment_savedpm_success" &&
                    req.params["duration"] == 1f &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onPaymentSuccess() for Google Pay payment should fire analytics request with expected event value`() {
        // Log initial event so that duration is tracked
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = false,
            activeLinkSession = false,
            currency = "usd",
            isDecoupling = false,
        )

        reset(analyticsRequestExecutor)
        whenever(eventTimeProvider.currentTimeMillis()).thenReturn(2000L)

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                isGooglePay = true,
            ),
            currency = "usd",
            isDecoupling = false,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_payment_googlepay_success" &&
                    req.params["duration"] == 1f
            }
        )
    }

    @Test
    fun `onPaymentFailure() should fire analytics request with expected event value`() {
        // Log initial event so that duration is tracked
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = false,
            activeLinkSession = false,
            currency = "usd",
            isDecoupling = false,
        )
        reset(analyticsRequestExecutor)
        whenever(eventTimeProvider.currentTimeMillis()).thenReturn(2000L)

        completeEventReporter.onPaymentFailure(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            currency = "usd",
            isDecoupling = false,
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_payment_savedpm_failure" &&
                    req.params["duration"] == 1f &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onSelectPaymentOption() should fire analytics request with expected event value`() {
        customEventReporter.onSelectPaymentOption(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            currency = "usd",
            isDecoupling = false,
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_custom_paymentoption_savedpm_select" &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `constructor does not read from PaymentConfiguration`() {
        PaymentConfiguration.clearInstance()
        // Would crash if it tries to read from the uninitialized PaymentConfiguration
        DefaultEventReporter(
            EventReporter.Mode.Complete,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            eventTimeProvider,
            testDispatcher
        )
    }
}
