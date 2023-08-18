package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class DefaultEventReporterTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val durationProvider = FakeDurationProvider()
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
            durationProvider,
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
            currency = "usd",
            isDecoupling = false,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_sheet_savedpm_show" &&
                    req.params["link_enabled"] == true &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onShowNewPaymentOptionForm() should fire analytics request with expected event value`() {
        completeEventReporter.onShowNewPaymentOptionForm(
            linkEnabled = false,
            currency = "usd",
            isDecoupling = false,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_sheet_newpm_show" &&
                    req.params["link_enabled"] == false &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onPaymentSuccess() should fire analytics request with expected event value`() {
        durationProvider.enqueueDuration(1.seconds)

        // Log initial event so that duration is tracked
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = false,
            currency = "usd",
            isDecoupling = false,
        )

        reset(analyticsRequestExecutor)

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            currency = "usd",
            deferredIntentConfirmationType = null,
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
        durationProvider.enqueueDuration(2.seconds)

        // Log initial event so that duration is tracked
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = false,
            currency = "usd",
            isDecoupling = false,
        )

        reset(analyticsRequestExecutor)

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                walletType = PaymentSelection.Saved.WalletType.GooglePay,
            ),
            currency = "usd",
            deferredIntentConfirmationType = null,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_payment_googlepay_success" &&
                    req.params["duration"] == 2f
            }
        )
    }

    @Test
    fun `onPaymentSuccess() for Link payment should fire analytics request with expected event value`() {
        durationProvider.enqueueDuration(123.milliseconds)

        // Log initial event so that duration is tracked
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = true,
            currency = "usd",
            isDecoupling = false,
        )

        reset(analyticsRequestExecutor)

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                walletType = PaymentSelection.Saved.WalletType.Link,
            ),
            currency = "usd",
            deferredIntentConfirmationType = null,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_payment_link_success" &&
                    req.params["duration"] == 0.123f
            }
        )
    }

    @Test
    fun `onPaymentFailure() should fire analytics request with expected event value`() {
        durationProvider.enqueueDuration(456.milliseconds)

        // Log initial event so that duration is tracked
        completeEventReporter.onShowExistingPaymentOptions(
            linkEnabled = false,
            currency = "usd",
            isDecoupling = false,
        )

        reset(analyticsRequestExecutor)

        completeEventReporter.onPaymentFailure(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            currency = "usd",
            isDecoupling = false,
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_payment_savedpm_failure" &&
                    req.params["duration"] == 0.456f &&
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
            durationProvider,
            testDispatcher
        )
    }
}
