package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.googlepay.StripeGooglePayEnvironment
import com.stripe.android.googlepay.StripeGooglePayLauncher
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
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
class DefaultPaymentSheetFlowControllerTest {
    private val paymentOptionCallback = mock<PaymentOptionCallback>()
    private val paymentResultCallback = mock<PaymentSheetResultCallback>()

    private val paymentController = mock<PaymentController>()
    private val eventReporter = mock<EventReporter>()
    private val flowController: DefaultPaymentSheetFlowController by lazy {
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
    fun `successful init() should fire analytics event`() {
        val flowController = createFlowController()
        flowController.init(
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
        val flowController = createFlowController(
            paymentMethods = paymentMethods,
            defaultPaymentMethodId = paymentMethods.first().id
        )
        flowController.init(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        assertThat(flowController.getPaymentOption())
            .isEqualTo(
                PaymentOption(
                    drawableResourceId = CardBrand.Visa.icon,
                    label = "Visa"
                )
            )
    }

    @Test
    fun `init with failure should return expected value`() {
        var result = Pair<Boolean, Throwable?>(false, null)
        createFlowController(
            FailingFlowControllerInitializer()
        ).init(PaymentSheetFixtures.CLIENT_SECRET) { isReady, error ->
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
        flowController.init(
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
                    config = null
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
        flowController.init(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
        )

        val expectedPaymentOption = PaymentOption(
            drawableResourceId = R.drawable.stripe_ic_visa,
            label = CardBrand.Visa.displayName
        )

        verify(paymentOptionCallback).onComplete(expectedPaymentOption)

        assertThat(flowController.getPaymentOption())
            .isEqualTo(expectedPaymentOption)
    }

    @Test
    fun `onPaymentOptionResult() with cancelled result should return invoke callback with null`() {
        flowController.init(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Cancelled(null)
        )

        verify(paymentOptionCallback).onComplete(isNull())
    }

    @Test
    fun `confirmPayment() without paymentSelection should not call paymentController`() {
        verifyNoMoreInteractions(paymentController)
        flowController.init(
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

        flowController.init(
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
                StripeGooglePayContract.Args(
                    environment = StripeGooglePayEnvironment.Test,
                    paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    countryCode = "US",
                    merchantName = "Widget Store"
                )
            )
    }

    @Test
    fun `isPaymentResult with Google Pay request code should return true`() {
        assertThat(
            flowController.isPaymentResult(
                StripeGooglePayLauncher.REQUEST_CODE,
                Intent()
            )
        ).isTrue()
    }

    @Test
    fun `onPaymentResult with Google Pay PaymentIntent result should invoke callback with Succeeded`() {
        val callback = mock<PaymentSheetResultCallback>()
        flowController.onPaymentResult(
            StripeGooglePayLauncher.REQUEST_CODE,
            Intent().putExtras(
                StripeGooglePayContract.Result.PaymentIntent(
                    paymentIntentResult = PAYMENT_INTENT_RESULT
                ).toBundle()
            ),
            callback = callback
        )
        verify(callback).onComplete(
            PaymentResult.Succeeded(PAYMENT_INTENT)
        )
        verify(eventReporter).onPaymentSuccess(PaymentSelection.GooglePay)
    }

    @Test
    fun `onPaymentResult with Google Pay Error result should invoke callback with Failed()`() {
        val callback = mock<PaymentSheetResultCallback>()
        flowController.onPaymentResult(
            StripeGooglePayLauncher.REQUEST_CODE,
            Intent().putExtras(
                StripeGooglePayContract.Result.Error(
                    exception = RuntimeException("Google Pay failed")
                ).toBundle()
            ),
            callback = callback
        )
        verify(callback).onComplete(
            argWhere { paymentResult ->
                (paymentResult as? PaymentResult.Failed)?.error?.message == "Google Pay failed"
            }
        )
        verify(eventReporter).onPaymentFailure(PaymentSelection.GooglePay)
    }

    @Test
    fun `onGooglePayResult() when canceled should invoke callback with canceled result`() {
        flowController.init(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onGooglePayResult(
            StripeGooglePayContract.Result.Canceled
        )

        verify(paymentResultCallback).onComplete(
            PaymentResult.Cancelled(null, null)
        )
    }

    @Test
    fun `onGooglePayResult() when payment intent result should invoke callback with canceled result`() {
        flowController.init(
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

        verify(paymentResultCallback).onComplete(
            PaymentResult.Succeeded(paymentIntent)
        )
    }

    @Test
    fun `init() when scope is cancelled before completion should not call onInit lambda`() {
        var onInitCallbacks = 0

        val flowController = createFlowController(
            FakeFlowControllerInitializer(
                emptyList(),
                delayMillis = 2000L
            )
        )
        flowController.init(PaymentSheetFixtures.CLIENT_SECRET) { _, _ ->
            onInitCallbacks++
        }

        testScope.advanceTimeBy(500L)
        testScope.cancel()

        assertThat(onInitCallbacks)
            .isEqualTo(0)
    }

    private fun createFlowController(
        paymentMethods: List<PaymentMethod> = emptyList(),
        defaultPaymentMethodId: String? = null
    ): DefaultPaymentSheetFlowController {
        return createFlowController(
            FakeFlowControllerInitializer(
                paymentMethods,
                defaultPaymentMethodId
            )
        )
    }

    private fun createFlowController(
        flowControllerInitializer: FlowControllerInitializer
    ): DefaultPaymentSheetFlowController {
        return DefaultPaymentSheetFlowController(
            activity,
            flowControllerInitializer,
            paymentController,
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
        private val defaultPaymentMethodId: String? = null,
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
                    defaultPaymentMethodId
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
                    null
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
        private val PAYMENT_INTENT_RESULT = PaymentIntentResult(PAYMENT_INTENT)
    }
}
