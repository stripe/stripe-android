package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.json.JSONException
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.time.Duration
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

    private val configuration: PaymentSheet.Configuration
        get() = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY

    @Test
    fun `onInit() should fire analytics request with expected event value`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete)

        completeEventReporter.onInit(
            configuration = configuration,
            isDeferred = false,
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
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateSuccessfulSetup()
        }

        completeEventReporter.onShowExistingPaymentOptions()

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
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateSuccessfulSetup(linkEnabled = false)
        }

        completeEventReporter.onShowNewPaymentOptionForm()

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
        // Log initial event so that duration is tracked
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateSuccessfulSetup()
            onShowExistingPaymentOptions()
        }

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
        // Log initial event so that duration is tracked
        val completeEventReporter = createEventReporter(
            mode = EventReporter.Mode.Complete,
            duration = 2.seconds,
        ) {
            simulateSuccessfulSetup()
            onShowExistingPaymentOptions()
        }

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                walletType = PaymentSelection.Saved.WalletType.GooglePay,
            ),
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
        // Log initial event so that duration is tracked
        val completeEventReporter = createEventReporter(
            mode = EventReporter.Mode.Complete,
            duration = 123.milliseconds,
        ) {
            simulateSuccessfulSetup()
            onShowExistingPaymentOptions()
        }

        completeEventReporter.onPaymentSuccess(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                walletType = PaymentSelection.Saved.WalletType.Link,
            ),
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
        // Log initial event so that duration is tracked
        val completeEventReporter = createEventReporter(
            mode = EventReporter.Mode.Complete,
            duration = 456.milliseconds,
        ) {
            simulateSuccessfulSetup()
            onShowExistingPaymentOptions()
        }

        completeEventReporter.onPaymentFailure(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            error = PaymentSheetConfirmationError.Stripe(APIException())
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_payment_savedpm_failure" &&
                    req.params["duration"] == 0.456f &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US" &&
                    req.params["error_message"] == "apiError"
            }
        )
    }

    @Test
    fun `onSelectPaymentOption() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onSelectPaymentOption(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
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
    fun `onShowEditablePaymentOption() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onShowEditablePaymentOption()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_open_edit_screen"
            }
        )
    }

    @Test
    fun `onHideEditablePaymentOption() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onHideEditablePaymentOption()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_cancel_edit_screen"
            }
        )
    }

    @Test
    fun `onShowPaymentOptionBrands() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onShowPaymentOptionBrands(
            source = EventReporter.CardBrandChoiceEventSource.Edit,
            selectedBrand = CardBrand.Visa
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_open_cbc_dropdown" &&
                    req.params["cbc_event_source"] == "edit" &&
                    req.params["selected_card_brand"] == "visa"
            }
        )
    }

    @Test
    fun `onHidePaymentOptionBrands() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onHidePaymentOptionBrands(
            source = EventReporter.CardBrandChoiceEventSource.Edit,
            selectedBrand = CardBrand.CartesBancaires,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_close_cbc_dropdown" &&
                    req.params["cbc_event_source"] == "edit" &&
                    req.params["selected_card_brand"] == "cartes_bancaires"
            }
        )
    }

    @Test
    fun `onUpdatePaymentMethodSucceeded() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onUpdatePaymentMethodSucceeded(
            selectedBrand = CardBrand.CartesBancaires,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_update_card" &&
                    req.params["selected_card_brand"] == "cartes_bancaires"
            }
        )
    }

    @Test
    fun `onUpdatePaymentMethodFailed() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onUpdatePaymentMethodFailed(
            selectedBrand = CardBrand.CartesBancaires,
            error = Exception("No network available!")
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_update_card_failed" &&
                    req.params["selected_card_brand"] == "cartes_bancaires" &&
                    req.params["error_message"] == "No network available!"
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

    @Test
    fun `Send correct error_message for server errors`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        completeEventReporter.onLoadFailed(
            error = JSONException("Server did something bad"),
        )

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        val errorType = argumentCaptor.firstValue.params["error_message"] as String
        assertThat(errorType).isEqualTo("apiError")
    }

    @Test
    fun `Send correct error_message for network errors`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        completeEventReporter.onLoadFailed(
            error = IOException("Internet no good"),
        )

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        val errorType = argumentCaptor.firstValue.params["error_message"] as String
        assertThat(errorType).isEqualTo("connectionError")
    }

    @Test
    fun `Send correct error_message for invalid requests`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        completeEventReporter.onLoadFailed(
            error = IllegalArgumentException("This ain't valid"),
        )

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        val errorType = argumentCaptor.firstValue.params["error_message"] as String
        assertThat(errorType).isEqualTo("invalidRequestError")
    }

    private fun createEventReporter(
        mode: EventReporter.Mode,
        duration: Duration = 1.seconds,
        configure: EventReporter.() -> Unit = {},
    ): EventReporter {
        val reporter = DefaultEventReporter(
            mode = mode,
            analyticsRequestExecutor = analyticsRequestExecutor,
            paymentAnalyticsRequestFactory = analyticsRequestFactory,
            durationProvider = FakeDurationProvider(duration),
            workContext = testDispatcher,
        )

        reporter.configure()

        reset(analyticsRequestExecutor)

        return reporter
    }

    private fun EventReporter.simulateInit() {
        onInit(configuration, isDeferred = false)
    }

    private fun EventReporter.simulateSuccessfulSetup(
        linkEnabled: Boolean = true,
        currency: String? = "usd",
    ) {
        onInit(configuration, isDeferred = false)
        onLoadStarted()
        onLoadSucceeded(linkEnabled, currency)
    }
}
