package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import android.graphics.Color
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentDetailsFixtures
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.DelicatePaymentSheetApi
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.paymentsheet.utils.RecordingGooglePayPaymentMethodLauncherFactory
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.IntentConfirmationInterceptorTestRule
import com.stripe.android.utils.RelayingPaymentSheetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
internal class DefaultFlowControllerTest {

    @get:Rule
    val intentConfirmationInterceptorRule = IntentConfirmationInterceptorTestRule()

    private val paymentOptionCallback = mock<PaymentOptionCallback>()
    private val paymentResultCallback = mock<PaymentSheetResultCallback>()

    private val paymentLauncherAssistedFactory = mock<StripePaymentLauncherAssistedFactory>()
    private val paymentLauncher = mock<StripePaymentLauncher>()
    private val eventReporter = mock<EventReporter>()

    private val paymentOptionActivityLauncher =
        mock<ActivityResultLauncher<PaymentOptionContract.Args>>()

    private val addressElementActivityLauncher =
        mock<ActivityResultLauncher<AddressElementActivityContract.Args>>()

    private val googlePayActivityLauncher =
        mock<ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>>()

    private val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()

    private val googlePayPaymentMethodLauncherFactory =
        RecordingGooglePayPaymentMethodLauncherFactory(googlePayPaymentMethodLauncher)

    private val linkActivityResultLauncher =
        mock<ActivityResultLauncher<LinkActivityContract.Args>>()

    private val sepaMandateActivityLauncher =
        mock<ActivityResultLauncher<SepaMandateContract.Args>>()

    private val linkPaymentLauncher = mock<LinkPaymentLauncher>()

    private val prefsRepository = FakePrefsRepository()

    private val lifeCycleOwner = mock<LifecycleOwner>()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val activityResultRegistry: ActivityResultRegistry = mock()

    private val activityResultRegistryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry
            get() = this@DefaultFlowControllerTest.activityResultRegistry
    }

    private val fakeIntentConfirmationInterceptor = FakeIntentConfirmationInterceptor()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        whenever(
            activityResultRegistry.register(
                any(),
                any<PaymentOptionContract>(),
                any()
            )
        ).thenReturn(paymentOptionActivityLauncher)

        whenever(
            activityResultRegistry.register(
                any(),
                any<AddressElementActivityContract>(),
                any()
            )
        ).thenReturn(addressElementActivityLauncher)

        whenever(
            activityResultRegistry.register(
                any(),
                any<GooglePayPaymentMethodLauncherContractV2>(),
                any()
            )
        ).thenReturn(googlePayActivityLauncher)

        whenever(
            activityResultRegistry.register(
                any(),
                any<LinkActivityContract>(),
                any()
            )
        ).thenReturn(linkActivityResultLauncher)

        whenever(
            activityResultRegistry.register(
                any(),
                any<SepaMandateContract>(),
                any()
            )
        ).thenReturn(sepaMandateActivityLauncher)

        whenever(
            activityResultRegistry.register(
                any(),
                any<PaymentLauncherContract>(),
                any()
            )
        ).thenReturn(mock())

        whenever(
            activityResultRegistry.register(
                any(),
                any<BacsMandateConfirmationContract>(),
                any()
            )
        ).thenReturn(mock())

        whenever(paymentLauncherAssistedFactory.create(any(), any(), anyOrNull(), any(), any()))
            .thenReturn(paymentLauncher)

        // set lifecycle to CREATED to trigger creation of payment launcher object within flowController.
        val lifecycle = LifecycleRegistry(lifeCycleOwner)
        lifecycle.currentState = Lifecycle.State.CREATED
        whenever(lifeCycleOwner.lifecycle).thenReturn(lifecycle)
    }

    @AfterTest
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful payment should fire analytics event`() = runTest {
        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess()

        viewModel.paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            mock(),
            mock()
        )

        flowController.onPaymentResult(PaymentResult.Completed)

        verify(eventReporter)
            .onPaymentSuccess(
                paymentSelection = isA<PaymentSelection.New>(),
                deferredIntentConfirmationType = isNull(),
            )
    }

    @Test
    fun `failed payment should fire analytics event`() = runTest {
        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess()

        viewModel.paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            mock(),
            mock()
        )

        val error = APIConnectionException()
        flowController.onPaymentResult(PaymentResult.Failed(error))

        verify(eventReporter)
            .onPaymentFailure(
                paymentSelection = isA<PaymentSelection.New>(),
                error = eq(PaymentSheetConfirmationError.Stripe(error)),
            )
    }

    @Test
    fun `Sends correct event for failed Google Pay payment`() = runTest {
        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess()

        viewModel.paymentSelection = PaymentSelection.GooglePay

        val errorCode = GooglePayPaymentMethodLauncher.INTERNAL_ERROR

        flowController.onGooglePayResult(
            GooglePayPaymentMethodLauncher.Result.Failed(
                error = RuntimeException(),
                errorCode = errorCode,
            )
        )

        verify(eventReporter).onPaymentFailure(
            paymentSelection = isA<PaymentSelection.GooglePay>(),
            error = eq(PaymentSheetConfirmationError.GooglePay(errorCode)),
        )
    }

    @Test
    fun `Sends correct event for invalid local state when confirming payment`() = runTest {
        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess()

        viewModel.state = null

        flowController.onGooglePayResult(
            GooglePayPaymentMethodLauncher.Result.Completed(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )
        )

        verify(eventReporter).onPaymentFailure(
            paymentSelection = isA<PaymentSelection.GooglePay>(),
            error = eq(PaymentSheetConfirmationError.InvalidState),
        )
    }

    @Test
    fun `getPaymentOption() when defaultPaymentMethodId is null should be null`() {
        val flowController = createFlowController()
        assertThat(flowController.getPaymentOption()).isNull()
    }

    @Test
    fun `getPaymentOption() when defaultPaymentMethodId is not null should return expected value`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(5)
        val last4 = paymentMethods.first().card?.last4.orEmpty()

        val flowController = createFlowController(
            paymentMethods = paymentMethods,
            paymentSelection = PaymentSelection.Saved(paymentMethods.first()),
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        assertThat(flowController.getPaymentOption())
            .isEqualTo(
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_ic_paymentsheet_card_visa,
                    label = "····$last4"
                )
            )
    }

    @Test
    fun `getPaymentOption() for new customer without saved payment methods returns null`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(5)
        val last4 = paymentMethods.first().card?.last4.orEmpty()

        // Initially configure for a customer with saved payment methods
        val paymentSheetLoader = FakePaymentSheetLoader(
            customerPaymentMethods = paymentMethods,
            paymentSelection = PaymentSelection.Saved(paymentMethods.first()),
        )

        val flowController = createFlowController(paymentSheetLoader)

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        assertThat(flowController.getPaymentOption())
            .isEqualTo(
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_ic_paymentsheet_card_visa,
                    label = "····$last4"
                )
            )

        // Simulate a real FlowControllerInitializer that fetches the payment methods for the new
        // customer, who doesn't have any saved payment methods
        paymentSheetLoader.updatePaymentMethods(emptyList())

        flowController.configureExpectingSuccess(
            clientSecret = PaymentSheetFixtures.DIFFERENT_CLIENT_SECRET,
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM,
        )

        // Should return null instead of any cached value from the previous customer
        assertThat(flowController.getPaymentOption()).isNull()
    }

    @Test
    fun `init with failure should return expected value`() = runTest {
        createFlowController(
            paymentSheetLoader = FakePaymentSheetLoader(shouldFail = true)
        ).configureExpectingError()
    }

    @Test
    fun `presentPaymentOptions() after successful init should launch with expected args`() = runTest {
        val flowController = createFlowController(linkState = null)

        flowController.configureExpectingSuccess()

        flowController.presentPaymentOptions()

        val expectedArgs = PaymentOptionContract.Args(
            state = PaymentSheetState.Full(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                customerPaymentMethods = emptyList(),
                config = PaymentSheet.Configuration("com.stripe.android.paymentsheet.test"),
                isGooglePayReady = false,
                paymentSelection = null,
                linkState = null,
                isEligibleForCardBrandChoice = false,
            ),
            statusBarColor = STATUS_BAR_COLOR,
            enableLogging = ENABLE_LOGGING,
            productUsage = PRODUCT_USAGE
        )

        verify(paymentOptionActivityLauncher).launch(eq(expectedArgs), anyOrNull())
    }

    @Test
    fun `presentPaymentOptions() without successful init should fail`() {
        val flowController = createFlowController()
        assertFailsWith<IllegalStateException> {
            flowController.presentPaymentOptions()
        }
    }

    @Test
    fun `onPaymentOptionResult() with saved payment method selection result should invoke callback with payment option`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        )

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
    fun `onPaymentOptionResult() with failure when initial value is a card invoke callback with last saved`() = runTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.GooglePay,
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

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
    fun `onPaymentOptionResult() with null invoke callback with null`() = runTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.GooglePay,
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.onPaymentOptionResult(null)

        verify(paymentOptionCallback).onPaymentOption(isNull())
    }

    @Test
    fun `onPaymentOptionResult() adds payment method which is added on next open`() = runTest {
        // Create a default flow controller with the paymentMethods initialized with cards.
        val initialPaymentMethods = PaymentMethodFixtures.createCards(5)
        val flowController = createFlowController(
            paymentMethods = initialPaymentMethods,
            paymentSelection = PaymentSelection.Saved(initialPaymentMethods.first())
        )
        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        // Add a saved card payment method so that we can make sure it is added when we open
        // up the payment option launcher
        flowController.onPaymentOptionResult(PaymentOptionResult.Succeeded(SAVE_NEW_CARD_SELECTION))
        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere {
                // Make sure that paymentMethods contains the new added payment methods and the initial payment methods.
                it.state.customerPaymentMethods == initialPaymentMethods
            },
            anyOrNull(),
        )
    }

    @Test
    fun `onPaymentOptionResult() with cancelled invoke callback when initial value is null`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Canceled(null, null)
        )

        verify(paymentOptionCallback).onPaymentOption(isNull())
    }

    @Test
    fun `onPaymentOptionResult() with cancelled invoke callback when initial value is a card`() = runTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.GooglePay,
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Canceled(null, PaymentSelection.GooglePay)
        )

        verify(paymentOptionCallback).onPaymentOption(
            PaymentOption(R.drawable.stripe_google_pay_mark, "Google Pay")
        )
    }

    @Test
    fun `confirmPayment() without paymentSelection should not call paymentLauncher`() = runTest {
        verifyNoMoreInteractions(paymentLauncher)

        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.confirm()
    }

    @Test
    fun `confirmPaymentSelection() with new card payment method should start paymentlauncher`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess()

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                paymentMethodOptions = PaymentMethodOptionsParams.Card()
            )
        )

        val initialSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )

        flowController.confirmPaymentSelection(
            NEW_CARD_PAYMENT_SELECTION,
            PaymentSheetState.Full(
                PaymentSheetFixtures.CONFIG_CUSTOMER,
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                customerPaymentMethods = PAYMENT_METHODS,
                isGooglePayReady = false,
                linkState = null,
                paymentSelection = initialSelection,
                isEligibleForCardBrandChoice = false,
            )
        )

        verifyPaymentSelection(
            PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            expectedPaymentMethodOptions = PaymentMethodOptionsParams.Card()
        )
    }

    @Test
    fun `confirmPaymentSelection() with generic payment method should start paymentLauncher`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess()

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = GENERIC_PAYMENT_SELECTION.paymentMethodCreateParams,
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                paymentMethodOptions = PaymentMethodOptionsParams.Card()
            )
        )

        val initialSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )

        flowController.confirmPaymentSelection(
            GENERIC_PAYMENT_SELECTION,
            PaymentSheetState.Full(
                PaymentSheetFixtures.CONFIG_CUSTOMER,
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                customerPaymentMethods = PAYMENT_METHODS,
                isGooglePayReady = false,
                linkState = null,
                paymentSelection = initialSelection,
                isEligibleForCardBrandChoice = false,
            )
        )

        verifyPaymentSelection(
            PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            GENERIC_PAYMENT_SELECTION.paymentMethodCreateParams
        )
    }

    @Test
    fun `confirmPaymentSelection() with us_bank_account payment method should start paymentLauncher`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess()

        val paymentSelection = GENERIC_PAYMENT_SELECTION.copy(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount()
            )
        )

        val initialSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )

        flowController.confirmPaymentSelection(
            paymentSelection,
            PaymentSheetState.Full(
                PaymentSheetFixtures.CONFIG_CUSTOMER,
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                customerPaymentMethods = PAYMENT_METHODS,
                isGooglePayReady = false,
                linkState = null,
                paymentSelection = initialSelection,
                isEligibleForCardBrandChoice = false,
            )
        )

        verifyPaymentSelection(
            PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            paymentSelection.paymentMethodCreateParams,
            PaymentMethodOptionsParams.USBankAccount()
        )
    }

    @Test
    fun `confirmPaymentSelection() with link payment method should launch LinkPaymentLauncher`() = runTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes.plus("link")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.confirm()

        verify(linkPaymentLauncher).present(any())
    }

    @Test
    fun `confirmPaymentSelection() with LinkInline and user not signed in should confirm with PaymentLauncher`() = runTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes.plus("link")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        val paymentSelection = PaymentSelection.New.LinkInline(
            LinkPaymentDetails.New(
                PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                mock(),
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                paymentSelection
            )
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET
            )
        )

        flowController.confirm()

        verify(paymentLauncher).confirm(any<ConfirmPaymentIntentParams>())
    }

    @Test
    fun `confirmPaymentSelection() with Link and shipping should have shipping details in confirm params`() = runTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes.plus("link")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        val paymentSelection = PaymentSelection.New.LinkInline(
            LinkPaymentDetails.New(
                PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                mock(),
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(paymentSelection)
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
                shipping = ConfirmPaymentIntentParams.Shipping(
                    name = "Test",
                    address = Address()
                )
            )
        )

        flowController.confirm()

        val paramsCaptor = argumentCaptor<ConfirmPaymentIntentParams>()

        verify(paymentLauncher).confirm(paramsCaptor.capture())

        assertThat(paramsCaptor.firstValue.toParamMap()["shipping"]).isEqualTo(
            mapOf(
                "address" to emptyMap<String, String>(),
                "name" to "Test"
            )
        )
    }

    @Test
    fun `confirmPaymentSelection() with Link and no shipping should not have shipping details in confirm params`() = runTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes.plus("link")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        val paymentSelection = PaymentSelection.New.LinkInline(
            LinkPaymentDetails.New(
                PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                mock(),
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(paymentSelection)
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET
            )
        )

        flowController.confirm()

        val paramsCaptor = argumentCaptor<ConfirmPaymentIntentParams>()

        verify(paymentLauncher).confirm(paramsCaptor.capture())

        assertThat(paramsCaptor.firstValue.toParamMap()["shipping"]).isNull()
    }

    @Test
    fun `confirm() with default sepa saved payment method should show sepa mandate`() = runTest {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val flowController = createFlowController(
            paymentSelection = paymentSelection,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes.plus("sepa_debit")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.copy(
                allowsDelayedPaymentMethods = true,
            )
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = mock(),
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET
            )
        )

        flowController.confirm()

        verify(sepaMandateActivityLauncher).launch(any())

        flowController.onSepaMandateResult(SepaMandateResult.Acknowledged)

        verify(paymentLauncher).confirm(any<ConfirmPaymentIntentParams>())
        flowController.onPaymentResult(PaymentResult.Completed)

        verify(paymentResultCallback).onPaymentSheetResult(PaymentSheetResult.Completed)
    }

    @Test
    fun `confirm() with default sepa saved payment method should cancel after show sepa mandate`() = runTest {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val flowController = createFlowController(
            paymentSelection = paymentSelection,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes.plus("sepa_debit")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.copy(
                allowsDelayedPaymentMethods = true,
            )
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = mock(),
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET
            )
        )

        flowController.confirm()

        verify(sepaMandateActivityLauncher).launch(any())

        flowController.onSepaMandateResult(SepaMandateResult.Canceled)

        verify(paymentLauncher, never()).confirm(any<ConfirmPaymentIntentParams>())

        verify(paymentResultCallback).onPaymentSheetResult(PaymentSheetResult.Canceled)
    }

    @Test
    fun `confirm() selecting sepa saved payment method`() = runTest {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val flowController = createFlowController(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes.plus("sepa_debit")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.copy(
                allowsDelayedPaymentMethods = true,
            )
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = mock(),
                clientSecret = PaymentSheetFixtures.CLIENT_SECRET
            )
        )

        flowController.onPaymentOptionResult(PaymentOptionResult.Succeeded(paymentSelection))

        flowController.confirm()

        verify(sepaMandateActivityLauncher, never()).launch(any())

        verify(paymentLauncher).confirm(any<ConfirmPaymentIntentParams>())
        flowController.onPaymentResult(PaymentResult.Completed)
        verify(paymentResultCallback).onPaymentSheetResult(PaymentSheetResult.Completed)
    }

    private fun verifyPaymentSelection(
        clientSecret: String,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        expectedPaymentMethodOptions: PaymentMethodOptionsParams? = PaymentMethodOptionsParams.Card()
    ) = runTest {
        val confirmPaymentIntentParams =
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                clientSecret = clientSecret,
                paymentMethodCreateParams = paymentMethodCreateParams,
                setupFutureUsage = null,
                shipping = null,
                savePaymentMethod = null,
                mandateId = null,
                mandateData = null,
                paymentMethodOptions = expectedPaymentMethodOptions
            )

        verify(paymentLauncher).confirm(
            eq(confirmPaymentIntentParams)
        )
    }

    @Test
    fun `confirmPayment() with GooglePay should launch GooglePayPaymentMethodLauncher`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )
        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )
        flowController.confirm()

        verify(googlePayPaymentMethodLauncher).present("usd", 1099L, "pi_1F7J1aCRMbs6FrXfaJcvbxF6")
    }

    @Test
    fun `onGooglePayResult() when canceled should invoke callback with canceled result`() = runTest {
        verifyNoInteractions(eventReporter)

        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.onGooglePayResult(
            GooglePayPaymentMethodLauncher.Result.Canceled
        )

        verify(paymentResultCallback).onPaymentSheetResult(
            PaymentSheetResult.Canceled
        )
    }

    @Test
    fun `onGooglePayResult() when PaymentData result should invoke confirm() with expected params`() =
        runTest {
            val flowController = createFlowController()

            flowController.configureExpectingSuccess(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            fakeIntentConfirmationInterceptor.enqueueConfirmStep(
                confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    paymentMethodId = PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!,
                    clientSecret = PaymentSheetFixtures.CLIENT_SECRET
                )
            )

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
    fun `confirmPayment() with Link should launch Link`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )
        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.Link)
        )
        flowController.confirm()

        verify(linkPaymentLauncher).present(any())
    }

    @Test
    fun `onLinkActivityResult() when canceled should invoke callback with canceled result`() = runTest {
        verifyNoInteractions(eventReporter)

        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.onLinkActivityResult(
            LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.BackPressed)
        )

        verify(paymentResultCallback).onPaymentSheetResult(
            PaymentSheetResult.Canceled
        )
    }

    @Test
    fun `onLinkActivityResult() when Completed result should invoke confirm()`() =
        runTest {
            val flowController = createFlowController()

            flowController.configureExpectingSuccess(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            fakeIntentConfirmationInterceptor.enqueueConfirmStep(
                confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    paymentMethodId = PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!,
                    clientSecret = PaymentSheetFixtures.CLIENT_SECRET
                )
            )

            flowController.onLinkActivityResult(
                LinkActivityResult.Completed(
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
    fun `onPaymentResult when succeeded should invoke callback with Completed`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess()

        flowController.onPaymentResult(PaymentResult.Completed)

        verify(paymentResultCallback).onPaymentSheetResult(
            argWhere { paymentResult ->
                paymentResult is PaymentSheetResult.Completed
            }
        )
    }

    @Test
    fun `onPaymentResult when canceled should invoke callback with Cancelled`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess()

        flowController.onPaymentResult(PaymentResult.Canceled)

        verify(paymentResultCallback).onPaymentSheetResult(
            argWhere { paymentResult ->
                paymentResult is PaymentSheetResult.Canceled
            }
        )
    }

    @Test
    fun `onPaymentResult when error should invoke callback with Failed and relay error message`() =
        runTest {
            val flowController = createFlowController()
            val errorMessage = "Original error message"

            flowController.onPaymentResult(PaymentResult.Failed(Throwable(errorMessage)))

            verify(paymentResultCallback).onPaymentSheetResult(
                argWhere { paymentResult ->
                    paymentResult is PaymentSheetResult.Failed &&
                        errorMessage == paymentResult.error.localizedMessage
                }
            )
        }

    @Test
    fun `Remembers previous new payment selection when presenting payment options again`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess()

        val previousPaymentSelection = NEW_CARD_PAYMENT_SELECTION

        flowController.onPaymentOptionResult(
            paymentOptionResult = PaymentOptionResult.Succeeded(previousPaymentSelection),
        )

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere { it.state.paymentSelection == previousPaymentSelection },
            anyOrNull(),
        )
    }

    @Test
    fun `Confirms intent if intent confirmation interceptor returns an unconfirmed intent`() {
        val flowController = createAndConfigureFlowControllerForDeferredIntent()

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                paymentSelection
            )
        )

        val expectedParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentSelection.paymentMethod.id!!,
            clientSecret = PaymentSheetFixtures.CLIENT_SECRET
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(expectedParams)

        flowController.confirm()

        verify(paymentLauncher).confirm(expectedParams)
    }

    @Test
    fun `Handles next action if intent confirmation interceptor returns an intent with an outstanding action`() {
        val flowController = createAndConfigureFlowControllerForDeferredIntent()

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                paymentSelection
            )
        )

        fakeIntentConfirmationInterceptor.enqueueNextActionStep(PaymentSheetFixtures.CLIENT_SECRET)

        flowController.confirm()

        verify(paymentLauncher).handleNextActionForPaymentIntent(PaymentSheetFixtures.CLIENT_SECRET)
    }

    @Test
    fun `Completes if intent confirmation interceptor returns a completed event`() {
        val flowController = createAndConfigureFlowControllerForDeferredIntent()

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                paymentSelection
            )
        )

        fakeIntentConfirmationInterceptor.enqueueCompleteStep()

        flowController.confirm()

        verify(paymentResultCallback).onPaymentSheetResult(PaymentSheetResult.Completed)
    }

    @Test
    fun `Returns failure if intent confirmation interceptor returns a failure`() {
        val flowController = createAndConfigureFlowControllerForDeferredIntent()

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                paymentSelection
            )
        )

        fakeIntentConfirmationInterceptor.enqueueFailureStep(
            cause = Exception("something went wrong"),
            message = "something went wrong"
        )

        flowController.confirm()

        verify(paymentResultCallback).onPaymentSheetResult(
            argWhere {
                (it as PaymentSheetResult.Failed).error.message == "something went wrong"
            }
        )
    }

    @Test
    fun `Returns failure if attempting to confirm while configure calls is in-flight`() = runTest {
        val mockLoader = RelayingPaymentSheetLoader()
        val flowController = createFlowController(paymentSheetLoader = mockLoader)

        mockLoader.enqueueSuccess()

        flowController.configureExpectingSuccess()

        // Simulate that the user has selected a payment method
        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )

        // Not enqueueing any loader response, so that the call is considered in-flight

        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Monsters, Inc.",
            ),
        ) { _, _ ->
            throw AssertionError("ConfirmCallback shouldn't have been called")
        }

        flowController.confirm()

        val expectedError = "FlowController.confirm() can only be called if the most " +
            "recent call to configureWithPaymentIntent(), configureWithSetupIntent() or " +
            "configureWithIntentConfiguration() has completed successfully."

        val argumentCaptor = argumentCaptor<PaymentSheetResult>()
        verify(paymentResultCallback).onPaymentSheetResult(argumentCaptor.capture())

        val result = argumentCaptor.firstValue as? PaymentSheetResult.Failed
        assertThat(result?.error?.message).isEqualTo(expectedError)
    }

    @Test
    fun `Returns failure if attempting to confirm if last configure call has failed`() = runTest {
        val mockLoader = RelayingPaymentSheetLoader()
        val flowController = createFlowController(paymentSheetLoader = mockLoader)

        mockLoader.enqueueSuccess()

        flowController.configureExpectingSuccess()

        // Simulate that the user has selected a payment method
        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )

        mockLoader.enqueueFailure()

        flowController.configureExpectingError(
            clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Monsters, Inc.",
            ),
        )

        flowController.confirm()

        val expectedError = "FlowController.confirm() can only be called if the most " +
            "recent call to configureWithPaymentIntent(), configureWithSetupIntent() or " +
            "configureWithIntentConfiguration() has completed successfully."

        val argumentCaptor = argumentCaptor<PaymentSheetResult>()
        verify(paymentResultCallback).onPaymentSheetResult(argumentCaptor.capture())

        val result = argumentCaptor.firstValue as? PaymentSheetResult.Failed
        assertThat(result?.error?.message).isEqualTo(expectedError)
    }

    @Test
    fun `Does not present payment options if last configure call has failed`() = runTest {
        val mockLoader = RelayingPaymentSheetLoader()
        val flowController = createFlowController(paymentSheetLoader = mockLoader)

        mockLoader.enqueueSuccess()

        flowController.configureExpectingSuccess()

        mockLoader.enqueueFailure()

        flowController.configureExpectingError(
            clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
            ),
        )

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher, never()).launch(any())
    }

    @Test
    fun `Does not present payment options if last configure call is in-flight`() = runTest {
        val mockLoader = RelayingPaymentSheetLoader()
        val flowController = createFlowController(paymentSheetLoader = mockLoader)

        mockLoader.enqueueSuccess()

        flowController.configureExpectingSuccess()

        // Not enqueueing any loader response, so that the call is considered in-flight

        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
            ),
        ) { _, _ ->
            throw AssertionError("ConfirmCallback shouldn't have been called")
        }

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher, never()).launch(any())
    }

    @OptIn(DelicatePaymentSheetApi::class)
    @Test
    fun `Sends correct analytics event based on force-success usage`() = runTest {
        val clientSecrets = listOf(
            PaymentSheet.IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT to DeferredIntentConfirmationType.None,
            "real_client_secret" to DeferredIntentConfirmationType.Server,
        )

        for ((clientSecret, deferredIntentConfirmationType) in clientSecrets) {
            IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
                CreateIntentResult.Success(clientSecret)
            }

            val flowController = createAndConfigureFlowControllerForDeferredIntent()
            val savedSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

            flowController.onPaymentOptionResult(
                PaymentOptionResult.Succeeded(savedSelection)
            )
            flowController.confirm()

            val isForceSuccess = clientSecret == PaymentSheet.IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT
            fakeIntentConfirmationInterceptor.enqueueCompleteStep(isForceSuccess)

            verify(eventReporter).onPaymentSuccess(
                paymentSelection = eq(savedSelection),
                deferredIntentConfirmationType = eq(deferredIntentConfirmationType),
            )
        }
    }

    @Test
    fun `Sends no deferred_intent_confirmation_type for non-deferred intent confirmation`() = runTest {
        val flowController = createFlowController().apply {
            configureExpectingSuccess()
        }

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(savedSelection)
        )
        flowController.confirm()

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethod.id!!,
            clientSecret = "pi_123_secret_456",
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(confirmParams)
        flowController.onPaymentResult(PaymentResult.Completed)

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            deferredIntentConfirmationType = isNull(),
        )
    }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for client-side confirmation of deferred intent`() = runTest {
        val flowController = createAndConfigureFlowControllerForDeferredIntent()

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(savedSelection)
        )
        flowController.confirm()

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethod.id!!,
            clientSecret = "pi_123_secret_456",
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = confirmParams,
            isDeferred = true,
        )
        flowController.onPaymentResult(PaymentResult.Completed)

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Client),
        )
    }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for server-side confirmation of deferred intent`() = runTest {
        val flowController = createAndConfigureFlowControllerForDeferredIntent()

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(savedSelection)
        )
        flowController.confirm()

        fakeIntentConfirmationInterceptor.enqueueNextActionStep("pi_123_secret_456")
        flowController.onPaymentResult(PaymentResult.Completed)

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Server),
        )
    }

    @Test
    fun `Launches Google Pay with custom label if provided for payment intent`() = runTest {
        val expectedLabel = "My custom label"
        val expectedAmount = 1099L

        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "My merchant",
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                    amount = 1234L,
                    label = expectedLabel,
                )
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )

        flowController.confirm()

        verify(googlePayPaymentMethodLauncher).present(
            currencyCode = any(),
            amount = eq(expectedAmount),
            transactionId = anyOrNull(),
            label = eq(expectedLabel),
        )
    }

    @Test
    fun `Launches Google Pay with custom label and amount if provided for setup intent`() = runTest {
        val expectedLabel = "My custom label"
        val expectedAmount = 1099L

        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            clientSecret = PaymentSheetFixtures.SETUP_CLIENT_SECRET,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "My merchant",
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                    amount = expectedAmount,
                    label = expectedLabel,
                )
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )

        flowController.confirm()

        verify(googlePayPaymentMethodLauncher).present(
            currencyCode = any(),
            amount = eq(expectedAmount),
            transactionId = anyOrNull(),
            label = eq(expectedLabel),
        )
    }

    @Test
    fun `Launches Bacs with name, email, sort code and account number & succeeds payment`() = runTest {
        fakeIntentConfirmationInterceptor.enqueueCompleteStep()

        val onResult = argumentCaptor<ActivityResultCallback<BacsMandateConfirmationResult>>()
        val launcher = mock<BacsMandateConfirmationLauncher> {
            on { launch(any(), any()) } doAnswer {
                onResult.firstValue.onActivityResult(BacsMandateConfirmationResult.Confirmed)
            }
        }
        val launcherFactory = mock<BacsMandateConfirmationLauncherFactory> {
            on { create(any()) } doReturn launcher
        }

        whenever(
            activityResultRegistry.register(
                any(),
                any<BacsMandateConfirmationContract>(),
                onResult.capture()
            )
        ).thenReturn(mock())

        val flowController = createFlowController(
            bacsMandateConfirmationLauncherFactory = launcherFactory
        )

        verify(launcherFactory).create(any())

        flowController.configureExpectingSuccess(
            clientSecret = PaymentSheetFixtures.SETUP_CLIENT_SECRET
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                createBacsPaymentSelection()
            )
        )

        flowController.confirm()

        verify(launcher).launch(
            eq(
                BacsMandateData(
                    name = BACS_NAME,
                    email = BACS_EMAIL,
                    sortCode = BACS_SORT_CODE,
                    accountNumber = BACS_ACCOUNT_NUMBER,
                )
            ),
            eq(PaymentSheet.Appearance())
        )

        verify(paymentResultCallback).onPaymentSheetResult(eq(PaymentSheetResult.Completed))
    }

    @Test
    fun `On complete internal payment result in PI mode & should reuse, should save payment selection`() = runTest {
        selectionSavedTest(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        ) { flowController ->
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_12345"
            ) { _, _ -> }
        }
    }

    @Test
    fun `On complete internal payment result in PI mode & should not reuse, should not save payment selection`() = runTest {
        selectionSavedTest(shouldSave = false) { flowController ->
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_12345"
            ) { _, _ -> }
        }
    }

    @Test
    fun `On complete internal payment result in SI mode, should save payment selection`() = runTest {
        selectionSavedTest { flowController ->
            flowController.configureWithSetupIntent(
                setupIntentClientSecret = "si_123456"
            ) { _, _ -> }
        }
    }

    @Test
    fun `On complete internal payment result with intent config in PI mode, should not save payment selection`() = runTest {
        selectionSavedTest(shouldSave = false) { flowController ->
            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD"
                    )
                )
            ) { _, _ -> }
        }
    }

    @Test
    fun `On complete internal payment result with intent config in PI+SFU mode, should save payment selection`() = runTest {
        selectionSavedTest { flowController ->
            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD",
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
                    )
                )
            ) { _, _ -> }
        }
    }

    @Test
    fun `On complete internal payment result with intent config in SI mode, should save payment selection`() = runTest {
        selectionSavedTest { flowController ->
            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "USD"
                    )
                )
            ) { _, _ -> }
        }
    }

    @Test
    fun `Requires email and phone with Google Pay when collection mode is set to always`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )

        flowController.confirm()

        val googlePayLauncherConfig = requireNotNull(googlePayPaymentMethodLauncherFactory.config)
        val isEmailRequired = googlePayLauncherConfig.isEmailRequired
        val isPhoneRequired = googlePayLauncherConfig.billingAddressConfig.isPhoneNumberRequired

        assertThat(isEmailRequired).isTrue()
        assertThat(isPhoneRequired).isTrue()
    }

    @Test
    fun `Does not require email and phone with Google Pay when collection mode is not set to always`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                ),
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(PaymentSelection.GooglePay)
        )

        flowController.confirm()

        val googlePayLauncherConfig = requireNotNull(googlePayPaymentMethodLauncherFactory.config)
        val isEmailRequired = googlePayLauncherConfig.isEmailRequired
        val isPhoneRequired = googlePayLauncherConfig.billingAddressConfig.isPhoneNumberRequired

        assertThat(isEmailRequired).isFalse()
        assertThat(isPhoneRequired).isFalse()
    }

    private suspend fun selectionSavedTest(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave =
            PaymentSelection.CustomerRequestedSave.NoRequest,
        shouldSave: Boolean = true,
        configure: (PaymentSheet.FlowController) -> Unit
    ) {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!
        val flowController = createFlowController()

        configure(flowController)

        val selection = PaymentSelection.New.Card(
            brand = CardBrand.Visa,
            customerRequestedSave = customerRequestedSave,
            paymentMethodCreateParams = PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParams.Card()
            )
        )

        flowController.onPaymentOptionResult(PaymentOptionResult.Succeeded(selection))
        flowController.onInternalPaymentResult(InternalPaymentResult.Completed(paymentIntent))

        val savedSelection = PaymentSelection.Saved(paymentIntent.paymentMethod!!)

        if (shouldSave) {
            assertThat(prefsRepository.paymentSelectionArgs).containsExactly(savedSelection)

            assertThat(
                prefsRepository.getSavedSelection(
                    isGooglePayAvailable = true,
                    isLinkAvailable = true
                )
            ).isEqualTo(
                SavedSelection.PaymentMethod(savedSelection.paymentMethod.id.orEmpty())
            )
        } else {
            assertThat(prefsRepository.paymentSelectionArgs).isEmpty()

            assertThat(
                prefsRepository.getSavedSelection(
                    isGooglePayAvailable = true,
                    isLinkAvailable = true
                )
            ).isEqualTo(SavedSelection.None)
        }
    }

    private fun createAndConfigureFlowControllerForDeferredIntent(
        paymentIntent: PaymentIntent = PaymentIntentFixtures.PI_SUCCEEDED,
    ): DefaultFlowController {
        val deferredIntent = paymentIntent.copy(id = null, clientSecret = null)
        return createFlowController(
            stripeIntent = deferredIntent
        ).apply {
            configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 12345,
                        currency = "usd"
                    )
                ),
                configuration = null,
                callback = { _, error ->
                    assertThat(error).isNull()
                },
            )
        }
    }

    private fun createFlowController(
        paymentMethods: List<PaymentMethod> = emptyList(),
        paymentSelection: PaymentSelection? = null,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        linkState: LinkState? = LinkState(
            configuration = mock(),
            loginState = LinkState.LoginState.LoggedIn,
        ),
        viewModel: FlowControllerViewModel = createViewModel(),
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory = mock()
    ): DefaultFlowController {
        return createFlowController(
            FakePaymentSheetLoader(
                customerPaymentMethods = paymentMethods,
                stripeIntent = stripeIntent,
                paymentSelection = paymentSelection,
                linkState = linkState,
            ),
            viewModel,
            bacsMandateConfirmationLauncherFactory
        )
    }

    private fun createFlowController(
        paymentSheetLoader: PaymentSheetLoader,
        viewModel: FlowControllerViewModel = createViewModel(),
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory = mock()
    ) = DefaultFlowController(
        viewModelScope = testScope,
        lifecycleOwner = lifeCycleOwner,
        activityResultRegistryOwner = activityResultRegistryOwner,
        statusBarColor = { STATUS_BAR_COLOR },
        paymentOptionFactory = PaymentOptionFactory(
            resources = context.resources,
            imageLoader = StripeImageLoader(context),
        ),
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        context = context,
        eventReporter = eventReporter,
        viewModel = viewModel,
        paymentLauncherFactory = paymentLauncherAssistedFactory,
        lazyPaymentConfiguration = {
            PaymentConfiguration.getInstance(context)
        },
        enableLogging = ENABLE_LOGGING,
        productUsage = PRODUCT_USAGE,
        googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
        prefsRepositoryFactory = { prefsRepository },
        bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
        linkLauncher = linkPaymentLauncher,
        configurationHandler = FlowControllerConfigurationHandler(
            paymentSheetLoader = paymentSheetLoader,
            uiContext = testDispatcher,
            eventReporter = eventReporter,
            viewModel = viewModel,
            paymentSelectionUpdater = { _, _, newState -> newState.paymentSelection },
        ),
        intentConfirmationInterceptor = fakeIntentConfirmationInterceptor,
    )

    private fun createViewModel(): FlowControllerViewModel {
        return FlowControllerViewModel(
            application = ApplicationProvider.getApplicationContext(),
            handle = SavedStateHandle(),
        )
    }

    private fun createBacsPaymentSelection(): PaymentSelection {
        return PaymentSelection.New.GenericPaymentMethod(
            labelResource = "Test",
            iconResource = 0,
            paymentMethodCreateParams = PaymentMethodCreateParams.Companion.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = BACS_ACCOUNT_NUMBER,
                    sortCode = BACS_SORT_CODE
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = BACS_NAME,
                    email = BACS_EMAIL
                )
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )
    }

    private companion object {
        private val NEW_CARD_PAYMENT_SELECTION = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Discover,
            PaymentSelection.CustomerRequestedSave.NoRequest
        )
        private val GENERIC_PAYMENT_SELECTION = PaymentSelection.New.GenericPaymentMethod(
            iconResource = R.drawable.stripe_ic_paymentsheet_card_visa,
            labelResource = "Bancontact",
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.BANCONTACT,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )
        private val VISA_PAYMENT_OPTION = PaymentOption(
            drawableResourceId = R.drawable.stripe_ic_paymentsheet_card_visa,
            label = "····4242"
        )

        private val SAVE_NEW_CARD_SELECTION = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)

        private const val ENABLE_LOGGING = false
        private val PRODUCT_USAGE = setOf("TestProductUsage")

        private val STATUS_BAR_COLOR = Color.GREEN

        private const val BACS_ACCOUNT_NUMBER = "00012345"
        private const val BACS_SORT_CODE = "108800"
        private const val BACS_NAME = "John Doe"
        private const val BACS_EMAIL = "johndoe@email.com"
    }
}

private suspend fun PaymentSheet.FlowController.configureExpectingSuccess(
    clientSecret: String = PaymentSheetFixtures.CLIENT_SECRET,
    configuration: PaymentSheet.Configuration? = null,
) {
    val configureTurbine = Turbine<Throwable?>()
    configureWithPaymentIntent(
        paymentIntentClientSecret = clientSecret,
        configuration = configuration,
    ) { _, error ->
        configureTurbine += error
    }
    assertThat(configureTurbine.awaitItem()).isNull()
}

private suspend fun PaymentSheet.FlowController.configureExpectingError(
    clientSecret: String = PaymentSheetFixtures.CLIENT_SECRET,
    configuration: PaymentSheet.Configuration? = null,
) {
    val configureTurbine = Turbine<Throwable?>()
    configureWithPaymentIntent(
        paymentIntentClientSecret = clientSecret,
        configuration = configuration,
    ) { _, error ->
        configureTurbine += error
    }
    assertThat(configureTurbine.awaitItem()).isNotNull()
}
