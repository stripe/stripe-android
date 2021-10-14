package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DefaultFlowControllerTest {
    private val paymentOptionCallback = mock<PaymentOptionCallback>()
    private val paymentResultCallback = mock<PaymentSheetResultCallback>()

    private val paymentLauncherAssistedFactory = mock<StripePaymentLauncherAssistedFactory>()
    private val paymentLauncher = mock<StripePaymentLauncher>()
    private val eventReporter = mock<EventReporter>()

    private val paymentOptionActivityLauncher =
        mock<ActivityResultLauncher<PaymentOptionContract.Args>>()

    private val googlePayActivityLauncher =
        mock<ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>>()
    val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()

    private val flowController: DefaultFlowController by lazy {
        createFlowController()
    }

    private val lifeCycleOwner = mock<LifecycleOwner>()

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher + Job())

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    private val activityResultCaller = mock<ActivityResultCaller>()

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

        whenever(
            activityResultCaller.registerForActivityResult(
                any<PaymentOptionContract>(),
                any()
            )
        ).thenReturn(paymentOptionActivityLauncher)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<GooglePayPaymentMethodLauncherContract>(),
                any()
            )
        ).thenReturn(googlePayActivityLauncher)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<PaymentLauncherContract>(),
                any()
            )
        ).thenReturn(mock())

        whenever(paymentLauncherAssistedFactory.create(any(), any(), any()))
            .thenReturn(paymentLauncher)

        // set lifecycle to CREATED to trigger creation of payment launcher object within flowController.
        val lifecycle = LifecycleRegistry(lifeCycleOwner)
        lifecycle.currentState = Lifecycle.State.CREATED
        whenever(lifeCycleOwner.lifecycle).thenReturn(lifecycle)
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun `successful configure() should fire analytics event`() {
        val flowController = createFlowController()
        flowController.configureWithPaymentIntent(
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
        flowController.configureWithPaymentIntent(
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
    fun `getPaymentOption() for new customer without saved payment methods returns null`() {
        val paymentMethods = PaymentMethodFixtures.createCards(5)
        val last4 = paymentMethods.first().card?.last4.orEmpty()

        // Initially configure for a customer with saved payment methods
        val flowControllerInitializer = FakeFlowControllerInitializer(
            paymentMethods = paymentMethods,
            savedSelection = SavedSelection.PaymentMethod(
                requireNotNull(paymentMethods.first().id)
            )
        )

        val flowController = createFlowController(flowControllerInitializer)

        flowController.configureWithPaymentIntent(
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

        // Simulate a real FlowControllerInitializer that fetches the payment methods for the new
        // customer, who doesn't have any saved payment methods
        flowControllerInitializer.paymentMethods = emptyList()

        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_MINIMUM
        ) { _, _ ->
        }
        // Should return null instead of any cached value from the previous customer
        assertThat(flowController.getPaymentOption())
            .isNull()
    }

    @Test
    fun `configure() with invalid paymentIntent`() {
        var result = Pair<Boolean, Throwable?>(true, null)
        val flowController = createFlowController()
        flowController.configureWithPaymentIntent(
            " ",
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("The PaymentIntent client_secret cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid merchant`() {
        var result = Pair<Boolean, Throwable?>(true, null)
        val flowController = createFlowController()
        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(merchantDisplayName = "")
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("When a Configuration is passed to PaymentSheet, the Merchant display name cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer id`() {
        var result = Pair<Boolean, Throwable?>(true, null)
        val flowController = createFlowController()
        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    id = " "
                )
            )
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("When a CustomerConfiguration is passed to PaymentSheet, the Customer ID cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid customer ephemeral key`() {
        var result = Pair<Boolean, Throwable?>(true, null)
        val flowController = createFlowController()
        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                customer = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.customer?.copy(
                    ephemeralKeySecret = " "
                )
            )
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("When a CustomerConfiguration is passed to PaymentSheet, the ephemeralKeySecret cannot be an empty string.")
    }

    @Test
    fun `configure() with invalid setupIntent`() {
        var result = Pair<Boolean, Throwable?>(true, null)
        val flowController = createFlowController()
        flowController.configureWithSetupIntent(
            " ",
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { success, error ->
            result = success to error
        }

        assertThat(result.first).isFalse()
        assertThat(result.second?.message)
            .isEqualTo("The SetupIntent client_secret cannot be an empty string.")
    }

    @Test
    fun `init with failure should return expected value`() {
        var result = Pair<Boolean, Throwable?>(false, null)
        createFlowController(
            FailingFlowControllerInitializer()
        ).configureWithPaymentIntent(PaymentSheetFixtures.CLIENT_SECRET) { isReady, error ->
            result = isReady to error
        }

        assertThat(result.first)
            .isFalse()
        assertThat(result.second)
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `presentPaymentOptions() after successful init should launch with expected args`() {
        var isReadyState = false
        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET
        ) { isReady, _ ->
            isReadyState = isReady
        }
        assertThat(isReadyState)
            .isTrue()
        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere {
                it == PaymentOptionContract.Args(
                    stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    paymentMethods = emptyList(),
                    config = null,
                    isGooglePayReady = false,
                    newCard = null,
                    statusBarColor = ContextCompat.getColor(
                        activity,
                        R.color.stripe_toolbar_color_default_dark
                    ),
                    injectorKey = INJECTOR_KEY,
                    enableLogging = ENABLE_LOGGING,
                    productUsage = PRODUCT_USAGE
                )
            }

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
        flowController.configureWithPaymentIntent(
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
    fun `onPaymentOptionResult() with failure when initial value is a card invoke callback with last saved`() {
        val flowController = createFlowController(
            savedSelection = SavedSelection.GooglePay
        )

        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Failed(Exception("Message for testing"))
        )

        verify(paymentOptionCallback).onPaymentOption(
            PaymentOption(
                R.drawable.stripe_google_pay_mark,
                "Google Pay"
            )
        )
    }

    @Test
    fun `onPaymentOptionResult() with null invoke callback with null`() {
        val flowController = createFlowController(
            savedSelection = SavedSelection.GooglePay
        )

        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onPaymentOptionResult(null)

        verify(paymentOptionCallback).onPaymentOption(isNull())
    }

    @Test
    fun `onPaymentOptionResult() adds payment method which is added on next open`() {
        // Create a default flow controller with the paymentMethods initialized with cards.
        val initialPaymentMethods = PaymentMethodFixtures.createCards(5)
        val flowController = createFlowController(
            paymentMethods = initialPaymentMethods,
            savedSelection = SavedSelection.PaymentMethod(
                requireNotNull(initialPaymentMethods.first().id)
            )
        )
        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        // Add a saved card payment method so that we can make sure it is added when we open
        // up the payment option launcher
        flowController.onPaymentOptionResult(PaymentOptionResult.Succeeded(SAVE_NEW_CARD_SELECTION))
        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere {
                // Make sure that paymentMethods contains the new added payment methods and the initial payment methods.
                it.paymentMethods == initialPaymentMethods
            }

        )
    }

    @Test
    fun `onPaymentOptionResult() with cancelled invoke callback when initial value is null`() {
        flowController.configureWithPaymentIntent(
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
    fun `onPaymentOptionResult() with cancelled invoke callback when initial value is a card`() {
        val flowController = createFlowController(
            savedSelection = SavedSelection.GooglePay
        )

        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Canceled(null)
        )

        verify(paymentOptionCallback).onPaymentOption(
            PaymentOption(R.drawable.stripe_google_pay_mark, "Google Pay")
        )
    }

    @Test
    fun `confirmPayment() without paymentSelection should not call paymentLauncher`() {
        verifyNoMoreInteractions(paymentLauncher)
        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        flowController.confirm()
    }

    @Test
    fun `confirmPaymentSelection() with new card payment method should start paymentlauncher`() =
        runBlockingTest {
            flowController.confirmPaymentSelection(
                NEW_CARD_PAYMENT_SELECTION,
                InitData(
                    PaymentSheetFixtures.CONFIG_CUSTOMER,
                    PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    listOf(PaymentMethod.Type.Card),
                    PAYMENT_METHODS,
                    SavedSelection.PaymentMethod(
                        id = "pm_123456789"
                    ),
                    isGooglePayReady = false
                )
            )

            verifyPaymentSelection(
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            )
        }

    @Test
    fun `confirmPaymentSelection() with generic payment method should start paymentLauncher`() {
        flowController.confirmPaymentSelection(
            GENERIC_PAYMENT_SELECTION,
            InitData(
                PaymentSheetFixtures.CONFIG_CUSTOMER,
                PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET,
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                listOf(PaymentMethod.Type.Card),
                PAYMENT_METHODS,
                SavedSelection.PaymentMethod(
                    id = "pm_123456789"
                ),
                isGooglePayReady = false
            )
        )

        verifyPaymentSelection(
            PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            GENERIC_PAYMENT_SELECTION.paymentMethodCreateParams
        )
    }

    private fun verifyPaymentSelection(
        clientSecret: String,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ) = runBlockingTest {
        val confirmPaymentIntentParams =
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                clientSecret = clientSecret,
                paymentMethodCreateParams = paymentMethodCreateParams,
                setupFutureUsage = null,
                shipping = null,
                savePaymentMethod = null,
                mandateId = null,
                mandateData = null,
            )

        verify(paymentLauncher).confirm(
            eq(confirmPaymentIntentParams),
        )
    }

    @Test
    fun `confirmPayment() with GooglePay should launch GooglePayPaymentMethodLauncher`() {
        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }
        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )
        flowController.confirm()

        verify(googlePayPaymentMethodLauncher).present("usd", 1099, "pi_1F7J1aCRMbs6FrXfaJcvbxF6")
    }

    @Test
    fun `onGooglePayResult() when canceled should invoke callback with canceled result`() {
        verifyZeroInteractions(eventReporter)

        flowController.configureWithPaymentIntent(
            PaymentSheetFixtures.CLIENT_SECRET,
            PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        ) { _, _ ->
        }

        flowController.onGooglePayResult(
            GooglePayPaymentMethodLauncher.Result.Canceled
        )

        verify(paymentResultCallback).onPaymentSheetResult(
            PaymentSheetResult.Canceled
        )
    }

    @Test
    fun `onGooglePayResult() when PaymentData result should invoke confirm() with expected params`() =
        testDispatcher.runBlockingTest {
            flowController.configureWithPaymentIntent(
                PaymentSheetFixtures.CLIENT_SECRET,
                PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ) { _, _ ->
            }

            flowController.onGooglePayResult(
                GooglePayPaymentMethodLauncher.Result.Completed(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )

            verify(paymentLauncher).confirm(
                argWhere { params: ConfirmPaymentIntentParams ->
                    params.paymentMethodId == "pm_123456789"
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
        flowController.configureWithPaymentIntent(PaymentSheetFixtures.CLIENT_SECRET) { _, _ ->
            onInitCallbacks++
        }

        testScope.advanceTimeBy(500L)
        testScope.cancel()

        assertThat(onInitCallbacks)
            .isEqualTo(0)
    }

    @Test
    fun `onPaymentResult when succeeded should invoke callback with Completed`() =
        testDispatcher.runBlockingTest {
            var isReadyState = false
            flowController.configureWithPaymentIntent(
                PaymentSheetFixtures.CLIENT_SECRET
            ) { isReady, error ->
                isReadyState = isReady
            }
            assertThat(isReadyState)
                .isTrue()

            flowController.onPaymentResult(PaymentResult.Completed)

            verify(paymentResultCallback).onPaymentSheetResult(
                argWhere { paymentResult ->
                    paymentResult is PaymentSheetResult.Completed
                }
            )
        }

    @Test
    fun `onPaymentResult when canceled should invoke callback with Cancelled`() =
        testDispatcher.runBlockingTest {
            var isReadyState = false
            flowController.configureWithPaymentIntent(
                PaymentSheetFixtures.CLIENT_SECRET
            ) { isReady, error ->
                isReadyState = isReady
            }
            assertThat(isReadyState)
                .isTrue()

            flowController.onPaymentResult(PaymentResult.Canceled)

            verify(paymentResultCallback).onPaymentSheetResult(
                argWhere { paymentResult ->
                    paymentResult is PaymentSheetResult.Canceled
                }
            )
        }

    @Test
    fun `onPaymentResult when error should invoke callback with Failed`() =
        testDispatcher.runBlockingTest {
            flowController.onPaymentResult(PaymentResult.Failed(Throwable("error")))

            verify(paymentResultCallback).onPaymentSheetResult(
                argWhere { paymentResult ->
                    paymentResult is PaymentSheetResult.Failed
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
    ) = DefaultFlowController(
        testScope,
        lifeCycleOwner,
        { activity.window.statusBarColor },
        PaymentOptionFactory(activity.resources),
        paymentOptionCallback,
        paymentResultCallback,
        activityResultCaller,
        INJECTOR_KEY,
        flowControllerInitializer,
        eventReporter,
        ViewModelProvider(activity)[FlowControllerViewModel::class.java],
        paymentLauncherAssistedFactory,
        mock(),
        { PaymentConfiguration.getInstance(activity) },
        testDispatcher,
        ENABLE_LOGGING,
        PRODUCT_USAGE,
        createGooglePayPaymentMethodLauncherFactory()
    )

    private fun createGooglePayPaymentMethodLauncherFactory() =
        object : GooglePayPaymentMethodLauncherFactory {
            override fun create(
                lifecycleScope: CoroutineScope,
                config: GooglePayPaymentMethodLauncher.Config,
                readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>,
                skipReadyCheck: Boolean
            ): GooglePayPaymentMethodLauncher {
                return googlePayPaymentMethodLauncher
            }
        }

    private class FakeFlowControllerInitializer(
        var paymentMethods: List<PaymentMethod>,
        private val savedSelection: SavedSelection = SavedSelection.None,
        private val delayMillis: Long = 0L,
        private val stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
    ) : FlowControllerInitializer {
        override suspend fun init(
            clientSecret: ClientSecret,
            paymentSheetConfiguration: PaymentSheet.Configuration?
        ): FlowControllerInitializer.InitResult {
            delay(delayMillis)
            return FlowControllerInitializer.InitResult.Success(
                InitData(
                    paymentSheetConfiguration,
                    clientSecret,
                    stripeIntent,
                    listOf(PaymentMethod.Type.Card),
                    paymentMethods,
                    savedSelection,
                    isGooglePayReady = false
                )
            )
        }
    }

    private class FailingFlowControllerInitializer :
        FlowControllerInitializer {
        override suspend fun init(
            clientSecret: ClientSecret,
            paymentSheetConfiguration: PaymentSheet.Configuration?
        ): FlowControllerInitializer.InitResult {
            return FlowControllerInitializer.InitResult.Failure(
                IllegalStateException("Failed to initialize")
            )
        }
    }

    private companion object {
        private val NEW_CARD_PAYMENT_SELECTION = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Discover,
            false
        )
        private val GENERIC_PAYMENT_SELECTION = PaymentSelection.New.GenericPaymentMethod(
            iconResource = R.drawable.stripe_ic_paymentsheet_card_visa,
            labelResource = R.drawable.stripe_ic_paymentsheet_pm_bancontact,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.BANCONTACT,
            userReuseRequest = false
        )
        private val VISA_PAYMENT_OPTION = PaymentOption(
            drawableResourceId = R.drawable.stripe_ic_paymentsheet_card_visa,
            label = "····4242"
        )

        private val SAVE_NEW_CARD_SELECTION = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            userReuseRequest = true
        )
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)

        private const val INJECTOR_KEY = "TestInjectorKey"
        private const val ENABLE_LOGGING = false
        private val PRODUCT_USAGE = setOf("TestProductUsage")
    }
}
