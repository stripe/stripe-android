package com.stripe.android.paymentsheet.analytics

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.analytics.experiment.LoggableExperiment.LinkHoldback.EmailRecognitionSource
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.toMap
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.IsStripeCardScanAvailable
import com.stripe.android.utils.AnalyticEventCallbackRule
import com.stripe.android.utils.FakeDurationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalAnalyticEventCallbackApi::class)
class DefaultEventReporterTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @get:Rule
    val analyticEventCallbackRule = AnalyticEventCallbackRule()

    private val durationProvider = FakeDurationProvider()
    private val analyticsRequestExecutor = mock<AnalyticsRequestExecutor>()
    private val analyticsV2RequestExecutor = FakeAnalyticsRequestV2Executor()
    private val analyticsRequestFactory = PaymentAnalyticsRequestFactory(
        ApplicationProvider.getApplicationContext(),
        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
    )

    private val fakeUserFacingLoggerCall = Turbine<String>()
    private val fakeUserFacingLogger = object : UserFacingLogger {
        override fun logWarningWithoutPii(message: String) {
            fakeUserFacingLoggerCall.add(message)
        }
    }

    private val configuration: PaymentSheet.Configuration
        get() = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY

    @Test
    fun `onInit() should fire analytics request with expected event value`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete)

        completeEventReporter.onInit(
            commonConfiguration = configuration.asCommonConfiguration(),
            appearance = configuration.appearance,
            primaryButtonColor = configuration.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(configuration),
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
    fun `on completed loading operation, should fire analytics request with expected event value`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_load_succeeded" &&
                    req.params["selected_lpm"] == "card"
            }
        )
    }

    @Test
    fun `on completed loading operation, should fire analytics with cvc recollection value`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            requireCvcRecollection = true
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["require_cvc_recollection"] == true
            }
        )
    }

    @Test
    fun `on completed loading operation, should reset checkout timer`() {
        val durationProvider = FakeDurationProvider()

        val eventReporter = createEventReporter(
            mode = EventReporter.Mode.Complete,
            durationProvider = durationProvider,
        )

        eventReporter.simulateSuccessfulSetup(
            PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        )

        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(
                    key = DurationProvider.Key.Checkout,
                    reset = true
                )
            )
        ).isTrue()
    }

    @Test
    fun `on completed loading operation, should fire analytics with hasDefaultPaymentMethod value`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            setAsDefaultEnabled = true,
            hasDefaultPaymentMethod = true
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["set_as_default_enabled"] == true &&
                    req.params["has_default_payment_method"] == true
            }
        )
    }

    @Test
    fun `on completed loading operation, should fire analytics with setAsDefaultEnabled value`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            setAsDefaultEnabled = true
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["set_as_default_enabled"] == true &&
                    req.params["has_default_payment_method"] == null
            }
        )
    }

    @Test
    fun `on completed loading operation, should fire analytics with pmo sfu map`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            paymentMethodOptionsSetupFutureUsage = true
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["payment_method_options_setup_future_usage"] == true
            }
        )
    }

    @Test
    fun `on completed loading operation, should fire analytics with setup future usage value`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            setupFutureUsage = StripeIntent.Usage.OffSession
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["setup_future_usage"] == "off_session"
            }
        )
    }

    @Test
    fun `onShowExistingPaymentOptions() should fire analytics request with expected event value`() =
        runTest(testDispatcher) {
            val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
                simulateSuccessfulSetup()
            }

            completeEventReporter.onShowExistingPaymentOptions()

            analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())
            verify(analyticsRequestExecutor).executeAsync(
                argWhere { req ->
                    req.params["event"] == "mc_complete_sheet_savedpm_show" &&
                        req.params["link_enabled"] == true &&
                        req.params["google_pay_enabled"] == true &&
                        req.params["currency"] == "usd" &&
                        req.params["locale"] == "en_US"
                }
            )
        }

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    @Test
    fun `onShowNewPaymentOptions() should fire analytics request with expected event value`() =
        runTest(testDispatcher) {
            val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
                simulateSuccessfulSetup(linkEnabled = false, googlePayReady = false)
            }

            completeEventReporter.onShowNewPaymentOptions()

            analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())
            verify(analyticsRequestExecutor).executeAsync(
                argWhere { req ->
                    req.params["event"] == "mc_complete_sheet_newpm_show" &&
                        req.params["link_enabled"] == false &&
                        req.params["google_pay_enabled"] == false &&
                        req.params["currency"] == "usd" &&
                        req.params["locale"] == "en_US"
                }
            )
        }

    @Test
    fun `onPaymentSuccess() should fire analytics request with expected event value`() = runTest(testDispatcher) {
        // Log initial event so that duration is tracked
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateSuccessfulSetup()
            onShowExistingPaymentOptions()
        }
        analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

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
    fun `onPaymentSuccess() for Google Pay payment should fire analytics request with expected event value`() =
        runTest(testDispatcher) {
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

            analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())
            verify(analyticsRequestExecutor).executeAsync(
                argWhere { req ->
                    req.params["event"] == "mc_complete_payment_googlepay_success" &&
                        req.params["duration"] == 2f
                }
            )
        }

    @Test
    fun `onPaymentSuccess() for Link payment should fire analytics request with expected event value`() =
        runTest(testDispatcher) {
            // Log initial event so that duration is tracked
            val completeEventReporter = createEventReporter(
                mode = EventReporter.Mode.Complete,
                duration = 123.milliseconds,
            ) {
                simulateSuccessfulSetup()
                onShowExistingPaymentOptions()
            }
            analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

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
    fun `onPaymentFailure() should fire analytics request with expected event value`() = runTest(testDispatcher) {
        // Log initial event so that duration is tracked
        val completeEventReporter = createEventReporter(
            mode = EventReporter.Mode.Complete,
            duration = 456.milliseconds,
        ) {
            simulateSuccessfulSetup()
            onShowExistingPaymentOptions()
        }
        analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.PresentedSheet())

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
    fun `onCannotProperlyReturnFromLinkAndOtherLPMs() should fire analytics request with expected event value`() {
        val completeEventReporter = createEventReporter(
            mode = EventReporter.Mode.Complete,
        ) {
            simulateSuccessfulSetup()
        }

        completeEventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_cannot_return_from_link_and_lpms"
            }
        )

        val customEventReporter = createEventReporter(
            mode = EventReporter.Mode.Custom,
        ) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_custom_cannot_return_from_link_and_lpms"
            }
        )
    }

    @Test
    fun `onSelectPaymentOption() should fire analytics request with expected event value`() = runTest(testDispatcher) {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onSelectPaymentOption(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )

        analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedSavedPaymentMethod("card"))
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_custom_paymentoption_savedpm_select" &&
                    req.params["currency"] == "usd" &&
                    req.params["locale"] == "en_US"
            }
        )
    }

    @Test
    fun `onPaymentMethodFormShown() should fire analytics request with expected event value`() =
        runTest(testDispatcher) {
            val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
                simulateSuccessfulSetup()
            }

            customEventReporter.onPaymentMethodFormShown(
                code = "card",
            )
            analyticEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.DisplayedPaymentMethodForm("card")
            )

            verify(analyticsRequestExecutor).executeAsync(
                argWhere { req ->
                    req.params["event"] == "mc_form_shown" &&
                        req.params["selected_lpm"] == "card"
                }
            )
        }

    @Test
    fun `onPaymentMethodFormShown() should restart duration on call`() = runTest(testDispatcher) {
        val durationProvider = FakeDurationProvider()

        val customEventReporter = createEventReporter(
            mode = EventReporter.Mode.Custom,
            durationProvider = durationProvider
        ) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onPaymentMethodFormShown(
            code = "card",
        )
        analyticEventCallbackRule.assertMatchesExpectedEvent(
            AnalyticEvent.DisplayedPaymentMethodForm("card")
        )

        assertThat(
            durationProvider.has(
                FakeDurationProvider.Call.Start(
                    key = DurationProvider.Key.ConfirmButtonClicked,
                    reset = true
                )
            )
        ).isTrue()
    }

    @Test
    fun `onPaymentMethodFormInteraction() should fire analytics request with expected event value`() =
        runTest(testDispatcher) {
            val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
                simulateSuccessfulSetup()
            }

            customEventReporter.onPaymentMethodFormInteraction(
                code = "card",
            )
            analyticEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.StartedInteractionWithPaymentMethodForm("card")
            )

            verify(analyticsRequestExecutor).executeAsync(
                argWhere { req ->
                    req.params["event"] == "mc_form_interacted" &&
                        req.params["selected_lpm"] == "card"
                }
            )
        }

    @Test
    fun `onPaymentMethodFormCompleted() should fire analytics request with expected event value`() =
        runTest(testDispatcher) {
            val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
                simulateSuccessfulSetup()
            }

            customEventReporter.onPaymentMethodFormCompleted(
                code = "card",
            )

            analyticEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.CompletedPaymentMethodForm(
                    paymentMethodType = "card"
                )
            )
        }

    @Test
    fun `onCardNumberCompleted() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onCardNumberCompleted()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_card_number_completed"
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
    fun `onBrandChoiceSelected(add) should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onBrandChoiceSelected(
            source = EventReporter.CardBrandChoiceEventSource.Add,
            selectedBrand = CardBrand.Visa
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_cbc_selected" &&
                    req.params["cbc_event_source"] == "add" &&
                    req.params["selected_card_brand"] == "visa"
            }
        )
    }

    @Test
    fun `onBrandChoiceSelected(edit) should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onBrandChoiceSelected(
            source = EventReporter.CardBrandChoiceEventSource.Edit,
            selectedBrand = CardBrand.Visa
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_cbc_selected" &&
                    req.params["cbc_event_source"] == "edit" &&
                    req.params["selected_card_brand"] == "visa"
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
    fun `onSetAsDefaultPaymentMethodSucceeded() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onSetAsDefaultPaymentMethodSucceeded(
            paymentMethodType = PaymentMethod.Type.Card.code,
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req: AnalyticsRequest ->
                req.params["event"] == "mc_set_default_payment_method" &&
                    req.params["payment_method_type"] == "card"
            }
        )
    }

    @Test
    fun `onExperimentExposure() should fire v2 analytics request with expected event value`() =
        runTest(testDispatcher) {
            val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
                simulateSuccessfulSetup()
            }

            val experiment = LoggableExperiment.LinkHoldback(
                experiment = LINK_GLOBAL_HOLD_BACK,
                arbId = "random_arb_id",
                isReturningLinkUser = false,
                group = "holdback",
                useLinkNative = true,
                emailRecognitionSource = EmailRecognitionSource.EMAIL,
                providedDefaultValues = LoggableExperiment.LinkHoldback.ProvidedDefaultValues(
                    email = true,
                    name = false,
                    phone = true,
                ),
                spmEnabled = true,
                integrationShape = "embedded",
                linkDisplayed = true
            )
            completeEventReporter.onExperimentExposure(experiment)

            val request = analyticsV2RequestExecutor.enqueueCalls.awaitItem()
            val params = request.params.toMap()

            assertEquals(request.eventName, "elements.experiment_exposure")
            assertEquals(params["experiment_retrieved"], "link_global_holdback")
            assertEquals(params["arb_id"], "random_arb_id")
            assertEquals(params["assignment_group"], "holdback")
            assertEquals(params["dimensions-integration_type"], "mpe_android")
            assertEquals(params["dimensions-is_returning_link_user"], "false")
            assertEquals(params["dimensions-recognition_type"], "email")
            assertEquals(params["dimensions-link_displayed"], "true")
            assertEquals(params["dimensions-dvs_provided"], "email phone")
            assertEquals(params["dimensions-integration_shape"], "embedded")
            assertEquals(params["sdk_platform"], "android")
            assertEquals(params["plugin_type"], "native")

            analyticsV2RequestExecutor.validate()
        }

    @Test
    fun `onSetAsDefaultPaymentMethodFailed() should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onSetAsDefaultPaymentMethodFailed(
            paymentMethodType = PaymentMethod.Type.Card.code,
            error = Exception("No network available!")
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_set_default_payment_method_failed" &&
                    req.params["payment_method_type"] == "card" &&
                    req.params["error_message"] == "No network available!"
            }
        )
    }

    @Test
    fun `onPressConfirmButton() should fire analytics request with expected event value`() = runTest(testDispatcher) {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onPressConfirmButton(
            PaymentSelection.New.GenericPaymentMethod(
                label = "Cash App Pay".resolvableString,
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            )
        )

        analyticEventCallbackRule.assertMatchesExpectedEvent(
            AnalyticEvent.TappedConfirmButton("cashapp")
        )
        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_confirm_button_tapped" &&
                    req.params["selected_lpm"] == "cashapp" &&
                    req.params["currency"] == "usd"
            }
        )
    }

    @Test
    fun `onDisallowedCardBrandEntered(brand) should fire analytics request with expected event value`() {
        val customEventReporter = createEventReporter(EventReporter.Mode.Custom) {
            simulateSuccessfulSetup()
        }

        customEventReporter.onDisallowedCardBrandEntered(CardBrand.AmericanExpress)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_disallowed_card_brand" &&
                    req.params["brand"] == "amex"
            }
        )
    }

    @Test
    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    fun `constructor does not read from PaymentConfiguration`() {
        PaymentConfiguration.clearInstance()
        // Would crash if it tries to read from the uninitialized PaymentConfiguration
        DefaultEventReporter(
            context = ApplicationProvider.getApplicationContext(),
            mode = EventReporter.Mode.Complete,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestV2Executor = analyticsV2RequestExecutor,
            paymentAnalyticsRequestFactory = analyticsRequestFactory,
            durationProvider = durationProvider,
            analyticEventCallbackProvider = analyticEventCallbackRule,
            workContext = testDispatcher,
            isStripeCardScanAvailable = FakeIsStripeCardScanAvailable(),
            logger = fakeUserFacingLogger,
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

    @Test
    fun `Send correct link_mode for payment method mode on load succeeded event`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            linkMode = LinkMode.LinkPaymentMethod
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_load_succeeded" &&
                    req.params["link_enabled"] == true &&
                    req.params["link_mode"] == "payment_method_mode"
            }
        )
    }

    @Test
    fun `Send correct link_mode for passthrough mode on load succeeded event`() {
        val eventReporter = createEventReporter(EventReporter.Mode.Complete)

        eventReporter.simulateSuccessfulSetup(
            linkMode = LinkMode.Passthrough
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_load_succeeded" &&
                    req.params["link_enabled"] == true &&
                    req.params["link_mode"] == "passthrough"
            }
        )
    }

    @Test
    fun `Send correct link_mode when selecting Bank payment method type for Instant Debits`() =
        runTest(testDispatcher) {
            val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
                simulateInit()
                simulateSuccessfulSetup(linkMode = LinkMode.LinkPaymentMethod)
            }

            completeEventReporter.onSelectPaymentMethod("link")

            analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedPaymentMethodType("link"))

            val argumentCaptor = argumentCaptor<AnalyticsRequest>()
            verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

            val errorType = argumentCaptor.firstValue.params["link_context"] as String
            assertThat(errorType).isEqualTo("instant_debits")
        }

    @Test
    fun `Send correct link_mode when selecting Bank payment method type for Link Card Brand`() =
        runTest(testDispatcher) {
            val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
                simulateInit()
                simulateSuccessfulSetup(linkMode = LinkMode.LinkCardBrand)
            }

            completeEventReporter.onSelectPaymentMethod("link")

            analyticEventCallbackRule.assertMatchesExpectedEvent(AnalyticEvent.SelectedPaymentMethodType("link"))

            val argumentCaptor = argumentCaptor<AnalyticsRequest>()
            verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

            val errorType = argumentCaptor.firstValue.params["link_context"] as String
            assertThat(errorType).isEqualTo("link_card_brand")
        }

    @Test
    fun `Send correct link_context when pressing confirm button for Instant Debits`() = runTest(testDispatcher) {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        val selection = mockUSBankAccountPaymentSelection(linkMode = LinkMode.LinkPaymentMethod)
        completeEventReporter.onPressConfirmButton(selection)

        analyticEventCallbackRule.assertMatchesExpectedEvent(
            AnalyticEvent.TappedConfirmButton("us_bank_account")
        )

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        val errorType = argumentCaptor.firstValue.params["link_context"] as String
        assertThat(errorType).isEqualTo("instant_debits")
    }

    @Test
    fun `Send correct link_context when pressing confirm button for Link Card Brand`() = runTest(testDispatcher) {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        val selection = mockUSBankAccountPaymentSelection(linkMode = LinkMode.LinkCardBrand)
        completeEventReporter.onPressConfirmButton(selection)

        analyticEventCallbackRule.assertMatchesExpectedEvent(
            AnalyticEvent.TappedConfirmButton("us_bank_account")
        )

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        val errorType = argumentCaptor.firstValue.params["link_context"] as String
        assertThat(errorType).isEqualTo("link_card_brand")
    }

    @Test
    fun `Send correct link_context when pressing confirm button for Link card payments`() = runTest(testDispatcher) {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        val selection = mockUSBankAccountPaymentSelection(linkMode = null)
        completeEventReporter.onPressConfirmButton(selection)

        analyticEventCallbackRule.assertMatchesExpectedEvent(
            AnalyticEvent.TappedConfirmButton("us_bank_account")
        )

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        assertThat(argumentCaptor.firstValue.params).doesNotContainKey("link_context")
    }

    @Test
    fun `Send correct link_context when on payment success for Instant Debits`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        val selection = mockUSBankAccountPaymentSelection(linkMode = LinkMode.LinkPaymentMethod)
        completeEventReporter.onPaymentSuccess(selection, deferredIntentConfirmationType = null)

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        val errorType = argumentCaptor.firstValue.params["link_context"] as String
        assertThat(errorType).isEqualTo("instant_debits")
    }

    @Test
    fun `Send correct link_context when on payment success for Link Card Brand`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        val selection = mockUSBankAccountPaymentSelection(linkMode = LinkMode.LinkCardBrand)
        completeEventReporter.onPaymentSuccess(selection, deferredIntentConfirmationType = null)

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        val errorType = argumentCaptor.firstValue.params["link_context"] as String
        assertThat(errorType).isEqualTo("link_card_brand")
    }

    @Test
    fun `Send correct link_context when on payment success for Link card payments`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        val selection = mockUSBankAccountPaymentSelection(linkMode = null)
        completeEventReporter.onPaymentSuccess(selection, deferredIntentConfirmationType = null)

        val argumentCaptor = argumentCaptor<AnalyticsRequest>()
        verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

        assertThat(argumentCaptor.firstValue.params).doesNotContainKey("link_context")
    }

    @Test
    fun `Send correct arguments when removing a saved payment method`() =
        runTest(testDispatcher) {
            val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
                simulateInit()
            }

            completeEventReporter.onRemoveSavedPaymentMethod("card")

            analyticEventCallbackRule.assertMatchesExpectedEvent(
                AnalyticEvent.RemovedSavedPaymentMethod("card")
            )
        }

    @Test
    fun `Sends general analytics event on call`() =
        runTest(testDispatcher) {
            val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
                simulateInit()
            }

            completeEventReporter.onAnalyticsEvent(PaymentAnalyticsEvent.FileCreate)

            val argumentCaptor = argumentCaptor<AnalyticsRequest>()
            verify(analyticsRequestExecutor).executeAsync(argumentCaptor.capture())

            val event = argumentCaptor.firstValue.params["event"]
            assertThat(event).isEqualTo(PaymentAnalyticsEvent.FileCreate.eventName)
        }

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    @Test
    fun `Throwable in analytic event callback should not be propagated`() = runTest(testDispatcher) {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateSuccessfulSetup(linkMode = null, googlePayReady = false)
        }

        val e = RuntimeException("Something went wrong")
        analyticEventCallbackRule.setCallback {
            @Suppress("TooGenericExceptionThrown")
            throw e
        }

        completeEventReporter.onShowNewPaymentOptions()

        assertThat(fakeUserFacingLoggerCall.awaitItem()).isEqualTo(
            "AnalyticEventCallback.onEvent() failed for event: PresentedSheet"
        )
    }

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    @Test
    fun `Null callback return by provider should not crash the app`() = runTest(testDispatcher) {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateSuccessfulSetup(linkMode = null, googlePayReady = false)
        }

        analyticEventCallbackRule.setCallback(null)

        completeEventReporter.onShowNewPaymentOptions()
    }

    @Test
    fun `onShopPayWebViewLoadAttempt should fire analytics request with expected event`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        completeEventReporter.onShopPayWebViewLoadAttempt()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_shoppay_webview_load_attempt"
            }
        )
    }

    @Test
    fun `onShopPayWebViewConfirmSuccess should fire analytics request with expected event`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        completeEventReporter.onShopPayWebViewConfirmSuccess()

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_shoppay_webview_confirm_success"
            }
        )
    }

    @Test
    fun `onShopPayWebViewCancelled should fire analytics request with expected event and params`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        completeEventReporter.onShopPayWebViewCancelled(didReceiveECEClick = true)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_shoppay_webview_cancelled" &&
                    req.params["did_receive_ece_click"] == true
            }
        )
    }

    @Test
    fun `onShopPayWebViewCancelled should fire analytics request with false ECE click param`() {
        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateInit()
        }

        completeEventReporter.onShopPayWebViewCancelled(didReceiveECEClick = false)

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_shoppay_webview_cancelled" &&
                    req.params["did_receive_ece_click"] == false
            }
        )
    }

    @Test
    fun `is_spt is false for all requests after load succeeded event`() = isSptTest(
        intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 5000L,
                currency = "CAD",
            ),
        ),
        expectedIsSptValue = false,
    )

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Test
    fun `is_spt is true for all requests after load succeeded event`() = isSptTest(
        intentConfiguration = PaymentSheet.IntentConfiguration(
            sharedPaymentTokenSessionWithMode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 5000L,
                currency = "CAD",
            ),
            sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                networkId = "network_id",
                externalId = "external_id",
            ),
        ),
        expectedIsSptValue = true,
    )

    private fun isSptTest(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        expectedIsSptValue: Boolean,
    ) {
        analyticEventCallbackRule.setCallback(null)

        val completeEventReporter = createEventReporter(EventReporter.Mode.Complete) {
            simulateSuccessfulSetup(
                initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                    intentConfiguration = intentConfiguration,
                )
            )
        }

        completeEventReporter.onShowNewPaymentOptions()
        completeEventReporter.onCardNumberCompleted()
        completeEventReporter.onPaymentMethodFormCompleted(code = "card")

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_complete_sheet_newpm_show" &&
                    req.params["is_spt"] == expectedIsSptValue
            }
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_card_number_completed" &&
                    req.params["is_spt"] == expectedIsSptValue
            }
        )

        verify(analyticsRequestExecutor).executeAsync(
            argWhere { req ->
                req.params["event"] == "mc_form_completed" &&
                    req.params["is_spt"] == expectedIsSptValue
            }
        )
    }

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    private fun createEventReporter(
        mode: EventReporter.Mode,
        duration: Duration = 1.seconds,
        configure: EventReporter.() -> Unit = {},
    ): EventReporter {
        val reporter = DefaultEventReporter(
            context = ApplicationProvider.getApplicationContext(),
            mode = mode,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestV2Executor = analyticsV2RequestExecutor,
            paymentAnalyticsRequestFactory = analyticsRequestFactory,
            durationProvider = FakeDurationProvider(duration),
            analyticEventCallbackProvider = analyticEventCallbackRule,
            workContext = testDispatcher,
            isStripeCardScanAvailable = FakeIsStripeCardScanAvailable(),
            logger = fakeUserFacingLogger,
        )

        reporter.configure()

        reset(analyticsRequestExecutor)

        return reporter
    }

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    private fun createEventReporter(
        mode: EventReporter.Mode,
        durationProvider: DurationProvider,
        configure: EventReporter.() -> Unit = {},
    ): EventReporter {
        val reporter = DefaultEventReporter(
            context = ApplicationProvider.getApplicationContext(),
            mode = mode,
            analyticsRequestExecutor = analyticsRequestExecutor,
            analyticsRequestV2Executor = analyticsV2RequestExecutor,
            paymentAnalyticsRequestFactory = analyticsRequestFactory,
            durationProvider = durationProvider,
            analyticEventCallbackProvider = analyticEventCallbackRule,
            workContext = testDispatcher,
            isStripeCardScanAvailable = FakeIsStripeCardScanAvailable(),
            logger = fakeUserFacingLogger,
        )

        reporter.configure()

        reset(analyticsRequestExecutor)

        return reporter
    }

    private fun EventReporter.simulateInit() {
        onInit(
            commonConfiguration = configuration.asCommonConfiguration(),
            appearance = configuration.appearance,
            primaryButtonColor = configuration.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(configuration),
            isDeferred = false
        )
    }

    private fun EventReporter.simulateSuccessfulSetup(
        paymentSelection: PaymentSelection = PaymentSelection.GooglePay,
        linkEnabled: Boolean = true,
        linkMode: LinkMode? = LinkMode.LinkPaymentMethod,
        googlePayReady: Boolean = true,
        currency: String? = "usd",
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "cs_example"
            ),
        requireCvcRecollection: Boolean = false,
        hasDefaultPaymentMethod: Boolean? = null,
        setAsDefaultEnabled: Boolean? = null,
        financialConnectionsAvailability: FinancialConnectionsAvailability = FinancialConnectionsAvailability.Full,
        linkDisplay: PaymentSheet.LinkConfiguration.Display = PaymentSheet.LinkConfiguration.Display.Automatic,
        paymentMethodOptionsSetupFutureUsage: Boolean = false,
        setupFutureUsage: StripeIntent.Usage? = null
    ) {
        simulateInit()
        onLoadStarted(initializedViaCompose = false)
        onLoadSucceeded(
            paymentSelection = paymentSelection,
            googlePaySupported = googlePayReady,
            linkEnabled = linkEnabled,
            linkMode = linkMode,
            currency = currency,
            initializationMode = initializationMode,
            orderedLpms = listOf("card", "klarna"),
            requireCvcRecollection = requireCvcRecollection,
            hasDefaultPaymentMethod = hasDefaultPaymentMethod,
            setAsDefaultEnabled = setAsDefaultEnabled,
            financialConnectionsAvailability = financialConnectionsAvailability,
            linkDisplay = linkDisplay,
            paymentMethodOptionsSetupFutureUsage = paymentMethodOptionsSetupFutureUsage,
            setupFutureUsage = setupFutureUsage
        )
    }

    private fun mockUSBankAccountPaymentSelection(linkMode: LinkMode?): PaymentSelection.New.USBankAccount {
        return PaymentSelection.New.USBankAccount(
            label = "Test",
            iconResource = 0,
            paymentMethodCreateParams = PaymentMethodCreateParams(
                code = PaymentMethod.Type.USBankAccount.code,
                requiresMandate = false,
            ),
            customerRequestedSave = mock(),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = null,
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
            instantDebits = PaymentSelection.New.USBankAccount.InstantDebitsInfo(
                paymentMethod = PaymentMethodFactory.instantDebits(),
                linkMode = linkMode,
            ).takeIf { it.linkMode != null },
            screenState = mock(),
        )
    }

    private class FakeIsStripeCardScanAvailable(
        private val value: Boolean = true
    ) : IsStripeCardScanAvailable {
        override fun invoke() = value
    }
}
