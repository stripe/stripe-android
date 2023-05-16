package com.stripe.android.paymentsheet.flowcontroller

import android.content.Context
import android.graphics.Color
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
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
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.testing.FakeIntentConfirmationInterceptor
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.RelayingPaymentSheetLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
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
    private val paymentOptionCallback = mock<PaymentOptionCallback>()
    private val paymentResultCallback = mock<PaymentSheetResultCallback>()

    private val paymentLauncherAssistedFactory = mock<StripePaymentLauncherAssistedFactory>()
    private val paymentLauncher = mock<StripePaymentLauncher> {
        on { authenticatorRegistry } doReturn mock()
    }
    private val eventReporter = mock<EventReporter>()

    private val paymentOptionActivityLauncher =
        mock<ActivityResultLauncher<PaymentOptionContract.Args>>()

    private val addressElementActivityLauncher =
        mock<ActivityResultLauncher<AddressElementActivityContract.Args>>()

    private val googlePayActivityLauncher =
        mock<ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>>()
    val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()

    private val linkActivityResultLauncher =
        mock<ActivityResultLauncher<LinkActivityContract.Args>>()

    private val linkPaymentLauncher = mock<LinkPaymentLauncher>()

    private val lifeCycleOwner = mock<LifecycleOwner>()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityResultCaller = mock<ActivityResultCaller>()

    private val fakeIntentConfirmationInterceptor = FakeIntentConfirmationInterceptor()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<PaymentOptionContract>(),
                any()
            )
        ).thenReturn(paymentOptionActivityLauncher)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<AddressElementActivityContract>(),
                any()
            )
        ).thenReturn(addressElementActivityLauncher)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<GooglePayPaymentMethodLauncherContract>(),
                any()
            )
        ).thenReturn(googlePayActivityLauncher)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<LinkActivityContract>(),
                any()
            )
        ).thenReturn(linkActivityResultLauncher)

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
                currency = eq("usd"),
                isDecoupling = eq(false),
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

        flowController.onPaymentResult(PaymentResult.Failed(RuntimeException()))

        verify(eventReporter)
            .onPaymentFailure(
                paymentSelection = isA<PaymentSelection.New>(),
                currency = eq("usd"),
                isDecoupling = eq(false),
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
                config = null,
                isGooglePayReady = false,
                paymentSelection = null,
                linkState = null,
            ),
            statusBarColor = STATUS_BAR_COLOR,
            injectorKey = INJECTOR_KEY,
            enableLogging = ENABLE_LOGGING,
            productUsage = PRODUCT_USAGE
        )

        verify(paymentOptionActivityLauncher).launch(eq(expectedArgs))
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
            }

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
        whenever(linkPaymentLauncher.getAccountStatusFlow(any())).thenReturn(flowOf(AccountStatus.Verified))
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

        verify(linkPaymentLauncher).present(any(), isNull())
    }

    @Test
    fun `confirmPaymentSelection() with LinkInline and user signed in should launch LinkPaymentLauncher`() = runTest {
        whenever(linkPaymentLauncher.getAccountStatusFlow(any())).thenReturn(flowOf(AccountStatus.Verified))

        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes.plus("link")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.onPaymentOptionResult(
            PaymentOptionResult.Succeeded(
                PaymentSelection.New.LinkInline(
                    LinkPaymentDetails.New(
                        PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                        mock(),
                        PaymentMethodCreateParamsFixtures.DEFAULT_CARD
                    )
                )
            )
        )

        flowController.confirm()

        verify(linkPaymentLauncher).present(any(), eq(PaymentMethodCreateParamsFixtures.DEFAULT_CARD))
    }

    @Test
    fun `confirmPaymentSelection() with LinkInline and user not signed in should confirm with PaymentLauncher`() = runTest {
        whenever(linkPaymentLauncher.getAccountStatusFlow(any())).thenReturn(flowOf(AccountStatus.SignedOut))

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
        whenever(linkPaymentLauncher.getAccountStatusFlow(any())).thenReturn(flowOf(AccountStatus.SignedOut))

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
        whenever(linkPaymentLauncher.getAccountStatusFlow(any())).thenReturn(flowOf(AccountStatus.SignedOut))

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

        verify(googlePayPaymentMethodLauncher).present("usd", 1099, "pi_1F7J1aCRMbs6FrXfaJcvbxF6")
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
            argWhere {
                it.state.paymentSelection == previousPaymentSelection
            }
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

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
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
    ): DefaultFlowController {
        return createFlowController(
            FakePaymentSheetLoader(
                customerPaymentMethods = paymentMethods,
                stripeIntent = stripeIntent,
                paymentSelection = paymentSelection,
                linkState = linkState,
            ),
            viewModel
        )
    }

    private fun createFlowController(
        paymentSheetLoader: PaymentSheetLoader,
        viewModel: FlowControllerViewModel = createViewModel(),
    ) = DefaultFlowController(
        viewModelScope = testScope,
        lifecycleOwner = lifeCycleOwner,
        statusBarColor = { STATUS_BAR_COLOR },
        paymentOptionFactory = PaymentOptionFactory(
            resources = context.resources,
            imageLoader = StripeImageLoader(context),
        ),
        paymentOptionCallback = paymentOptionCallback,
        paymentResultCallback = paymentResultCallback,
        activityResultCaller = activityResultCaller,
        injectorKey = INJECTOR_KEY,
        eventReporter = eventReporter,
        viewModel = viewModel,
        paymentLauncherFactory = paymentLauncherAssistedFactory,
        lazyPaymentConfiguration = {
            PaymentConfiguration.getInstance(context)
        },
        enableLogging = ENABLE_LOGGING,
        productUsage = PRODUCT_USAGE,
        googlePayPaymentMethodLauncherFactory = createGooglePayPaymentMethodLauncherFactory(),
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

        private const val INJECTOR_KEY = "TestInjectorKey"
        private const val ENABLE_LOGGING = false
        private val PRODUCT_USAGE = setOf("TestProductUsage")

        private val STATUS_BAR_COLOR = Color.GREEN
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
