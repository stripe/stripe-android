package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.FakePaymentFlowResultProcessor
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DefaultFlowControllerTest {
    private val paymentOptionCallback = mock<PaymentOptionCallback>()
    private val paymentResultCallback = mock<PaymentSheetResultCallback>()

    private val paymentController = mock<PaymentController>()
    private val eventReporter = mock<EventReporter>()

    private val paymentFlowResultProcessor = FakePaymentFlowResultProcessor()
    private val flowController: DefaultFlowController by lazy {
        createFlowController()
    }

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher + Job())

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private lateinit var activity: ComponentActivity

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val activityScenario = activityScenarioFactory.createAddPaymentMethodActivity()
        activityScenario.moveToState(Lifecycle.State.CREATED)
        activityScenario.onActivity {
            activity = it
        }
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun `successful configure() should fire analytics event`() {
        val flowController = createFlowController()
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        verify(eventReporter)
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `getPaymentOption() when defaultPaymentMethodId is null should be null`() {
        assertThat(flowController.getPaymentOption())
            .isNull()
    }

    @Test
    fun `getPaymentOption() when defaultPaymentMethodId is not null should return expected value`() {
        val paymentMethods = PaymentMethodFixtures.createCards(5)
        val last4 = paymentMethods.first().card?.last4.orEmpty()

        val flowController = createFlowController(
            paymentMethods = paymentMethods,
            savedSelection = SavedSelection.PaymentMethod(
                requireNotNull(paymentMethods.first().id)
            )
        )
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        assertThat(flowController.getPaymentOption())
            .isEqualTo(
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_ic_paymentsheet_card_visa,
                    label = "····$last4"
                )
            )
    }

    @Test
    fun `init with failure should return expected value`() {
        var result = Pair<Boolean, Throwable?>(false, null)
        createFlowController(
            FailingFlowControllerInitializer()
        ).configure(PaymentSheetFixtures.CLIENT_SECRET) { isReady, error ->
            result = isReady to error
        }

        assertThat(result.first)
            .isFalse()
        assertThat(result.second)
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `presentPaymentOptions() after successful init should launch with expected args`() {
        var launchArgs: PaymentOptionContract.Args? = null
        flowController.paymentOptionLauncher = {
            launchArgs = it
        }

        var isReadyState = false
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET
        ) { isReady, _ ->
            isReadyState = isReady
        }
        assertThat(isReadyState)
            .isTrue()
        flowController.presentPaymentOptions()

        assertThat(launchArgs)
            .isEqualTo(
                PaymentOptionContract.Args(
                    paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    paymentMethods = emptyList(),
                    sessionId = SESSION_ID,
                    config = null,
                    isGooglePayReady = false,
                    newCard = null
                )
            )
    }

    @Test
    fun `presentPaymentOptions() without successful init should fail`() {
        assertFailsWith<IllegalStateException> {
            flowController.presentPaymentOptions()
        }
    }

    @Test
    fun `onPaymentOptionResult() with saved payment method selection result should invoke callback with payment option`() {
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        )

        verify(paymentOptionCallback).onPaymentOption(VISA_PAYMENT_OPTION)
        assertThat(flowController.getPaymentOption())
            .isEqualTo(VISA_PAYMENT_OPTION)
    }

    @Test
    fun `onPaymentOptionResult() with cancelled result should return invoke callback with null`() {
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Canceled(null)
        )

        verify(paymentOptionCallback).onPaymentOption(isNull())
    }

    @Test
    fun `confirmPayment() without paymentSelection should not call paymentController`() {
        verifyNoMoreInteractions(paymentController)
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        flowController.confirmPayment()
    }

    @Test
    fun `confirmPayment() with GooglePay should start StripeGooglePayLauncher`() {
        var launchArgs: StripeGooglePayContract.Args? = null
        flowController.googlePayLauncher = {
            launchArgs = it
        }

        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )
        flowController.confirmPayment()
        assertThat(launchArgs)
            .isEqualTo(
                StripeGooglePayContract.Args.PaymentData(
                    paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    config = StripeGooglePayContract.GooglePayConfig(
                        environment = StripeGooglePayEnvironment.Test,
                        countryCode = "US",
                        merchantName = "Widget Store"
                    )
                )
            )
    }

    @Test
    fun `onGooglePayResult() when canceled should invoke callback with canceled result`() {
        verifyZeroInteractions(eventReporter)

        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onGooglePayResult(
            StripeGooglePayContract.Result.Canceled
        )

        verify(paymentResultCallback).onPaymentResult(
            PaymentResult.Canceled(null, null)
        )
    }

    @Test
    fun `onGooglePayResult() when payment intent result should invoke callback with Completed result`() {
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        val paymentIntent = PAYMENT_INTENT.copy(status = StripeIntent.Status.Succeeded)
        flowController.onGooglePayResult(
            StripeGooglePayContract.Result.PaymentIntent(
                PaymentIntentResult(
                    intent = paymentIntent,
                    outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
                )
            )
        )

        verify(eventReporter).onPaymentSuccess(PaymentSelection.GooglePay)
        verify(paymentResultCallback).onPaymentResult(
            PaymentResult.Completed(paymentIntent)
        )
    }

    @Test
    fun `onGooglePayResult() when payment intent result has lastPaymentError should invoke callback with failure result`() {
        flowController.configure(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        val paymentIntent = PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR
        flowController.onGooglePayResult(
            StripeGooglePayContract.Result.PaymentIntent(
                PaymentIntentResult(
                    intent = paymentIntent,
                    outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
                )
            )
        )

        verify(eventReporter).onPaymentFailure(PaymentSelection.GooglePay)
        verify(paymentResultCallback).onPaymentResult(
            argWhere { paymentResult ->
                when (paymentResult) {
                    is PaymentResult.Failed -> {
                        paymentResult.error.message == "Failed to confirm PaymentIntent. The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again." &&
                            paymentResult.paymentIntent == PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR
                    }
                    else -> false
                }
            }
        )
    }

    @Test
    fun `configure() when scope is cancelled before completion should not call onInit lambda`() {
        var onInitCallbacks = 0

        val flowController = createFlowController(
            FakeFlowControllerInitializer(
                emptyList(),
                delayMillis = 2000L
            )
        )
        flowController.configure(PaymentSheetFixtures.CLIENT_SECRET) { _, _ ->
            onInitCallbacks++
        }

        testScope.advanceTimeBy(500L)
        testScope.cancel()

        assertThat(onInitCallbacks)
            .isEqualTo(0)
    }

    @Test
    fun `onPaymentFlowResult when succeeded should invoke callback with Completed`() {
        paymentFlowResultProcessor.paymentIntentResult = PaymentIntentResult(
            PaymentIntentFixtures.PI_WITH_SHIPPING,
            StripeIntentResult.Outcome.SUCCEEDED
        )

        flowController.onPaymentFlowResult(
            PaymentFlowResult.Unvalidated(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                flowOutcome = StripeIntentResult.Outcome.CANCELED
            )
        )

        verify(paymentResultCallback).onPaymentResult(
            argWhere { paymentResult ->
                (paymentResult as? PaymentResult.Completed)?.paymentIntent ==
                    PaymentIntentFixtures.PI_WITH_SHIPPING
            }
        )
    }

    @Test
    fun `onPaymentFlowResult when canceled should invoke callback with Cancelled`() {
        paymentFlowResultProcessor.paymentIntentResult = PaymentIntentResult(
            PaymentIntentFixtures.CANCELLED,
            StripeIntentResult.Outcome.CANCELED
        )

        flowController.onPaymentFlowResult(
            PaymentFlowResult.Unvalidated(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                flowOutcome = StripeIntentResult.Outcome.CANCELED
            )
        )

        verify(paymentResultCallback).onPaymentResult(
            argWhere { paymentResult ->
                (paymentResult as? PaymentResult.Canceled)?.paymentIntent ==
                    PaymentIntentFixtures.CANCELLED
            }
        )
    }

    @Test
    fun `onPaymentFlowResult when error should invoke callback with Failed`() {
        paymentFlowResultProcessor.paymentIntentResult = PaymentIntentResult(
            PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR,
            StripeIntentResult.Outcome.FAILED
        )

        flowController.onPaymentFlowResult(
            PaymentFlowResult.Unvalidated(
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                flowOutcome = StripeIntentResult.Outcome.CANCELED
            )
        )

        verify(paymentResultCallback).onPaymentResult(
            argWhere { paymentResult ->
                (paymentResult as? PaymentResult.Failed)?.paymentIntent ==
                    PaymentIntentFixtures.PI_WITH_LAST_PAYMENT_ERROR
            }
        )
    }

    private fun createFlowController(
        paymentMethods: List<PaymentMethod> = emptyList(),
        savedSelection: SavedSelection = SavedSelection.None
    ): DefaultFlowController {
        return createFlowController(
            FakeFlowControllerInitializer(
                paymentMethods,
                savedSelection
            )
        )
    }

    private fun createFlowController(
        flowControllerInitializer: FlowControllerInitializer
    ): DefaultFlowController {
        return DefaultFlowController(
            activity,
            flowControllerInitializer,
            { _, _, _ -> paymentController },
            paymentFlowResultProcessor,
            eventReporter,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            null,
            sessionId = SESSION_ID,
            testScope,
            paymentOptionCallback,
            paymentResultCallback
        )
    }

    private class FakeFlowControllerInitializer(
        private val paymentMethods: List<PaymentMethod>,
        private val savedSelection: SavedSelection = SavedSelection.None,
        private val delayMillis: Long = 0L
    ) : FlowControllerInitializer {
        override suspend fun init(
            paymentIntentClientSecret: String,
            configuration: PaymentSheet.Configuration
        ): FlowControllerInitializer.InitResult {
            delay(delayMillis)
            return FlowControllerInitializer.InitResult.Success(
                InitData(
                    configuration,
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    listOf(PaymentMethod.Type.Card),
                    paymentMethods,
                    savedSelection,
                    isGooglePayReady = false
                )
            )
        }

        override suspend fun init(
            paymentIntentClientSecret: String
        ): FlowControllerInitializer.InitResult {
            delay(delayMillis)
            return FlowControllerInitializer.InitResult.Success(
                InitData(
                    null,
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    listOf(PaymentMethod.Type.Card),
                    emptyList(),
                    SavedSelection.None,
                    isGooglePayReady = false
                )
            )
        }
    }

    private class FailingFlowControllerInitializer : FlowControllerInitializer {
        override suspend fun init(
            paymentIntentClientSecret: String,
            configuration: PaymentSheet.Configuration
        ): FlowControllerInitializer.InitResult {
            return FlowControllerInitializer.InitResult.Failure(
                IllegalStateException("Failed to initialize")
            )
        }

        override suspend fun init(
            paymentIntentClientSecret: String
        ): FlowControllerInitializer.InitResult {
            return FlowControllerInitializer.InitResult.Failure(
                IllegalStateException("Failed to initialize")
            )
        }
    }

    private companion object {
        private val SESSION_ID = SessionId()

        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        private val VISA_PAYMENT_OPTION = PaymentOption(
            drawableResourceId = R.drawable.stripe_ic_paymentsheet_card_visa,
            label = "····4242"
        )
    }
}
