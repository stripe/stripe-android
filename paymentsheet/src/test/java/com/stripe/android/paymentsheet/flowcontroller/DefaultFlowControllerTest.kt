package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import android.graphics.Color
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_SESSION
import com.stripe.android.link.TestFactory.VERIFICATION_STARTED_SESSION
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.confirmation.linkinline.LinkInlineSignupConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.FakePrefsRepository
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionResultCallback
import com.stripe.android.paymentsheet.PaymentOptionsActivityResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.PaymentOptionFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.SepaMandateContract
import com.stripe.android.paymentsheet.ui.SepaMandateResult
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.PaymentElementCallbackTestRule
import com.stripe.android.utils.RelayingPaymentElementLoader
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class DefaultFlowControllerTest {

    @get:Rule
    val paymentElemntCallbackTestRule = PaymentElementCallbackTestRule()

    private val paymentOptionResultCallback = mock<PaymentOptionResultCallback>()
    private val paymentResultCallback = mock<PaymentSheetResultCallback>()
    private val eventReporter = mock<EventReporter>()

    private val paymentOptionActivityLauncher =
        mock<ActivityResultLauncher<PaymentOptionContract.Args>>()

    private val sepaMandateActivityLauncher =
        mock<ActivityResultLauncher<SepaMandateContract.Args>>()

    private val flowControllerLinkPaymentLauncher = mock<LinkPaymentLauncher>()
    private val walletsButtonLinkPaymentLauncher = mock<LinkPaymentLauncher>()

    private val prefsRepository = FakePrefsRepository()

    private val lifecycleOwner = TestLifecycleOwner()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val context = application.applicationContext

    private val activityResultCaller: ActivityResultCaller = mock()

    private val linkGate = FakeLinkGate()

    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Suppress("LongMethod")
    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(application.applicationContext, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<PaymentOptionContract>(),
                any()
            )
        ).thenReturn(paymentOptionActivityLauncher)

        whenever(
            activityResultCaller.registerForActivityResult(
                any<SepaMandateContract>(),
                any()
            )
        ).thenReturn(sepaMandateActivityLauncher)

        lifecycleOwner.currentState = Lifecycle.State.RESUMED
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
    fun `successful payment should clear viewmodel state`() = runTest {
        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess()

        viewModel.paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            mock(),
            mock()
        )

        flowController.onPaymentResult(PaymentResult.Completed)

        assertThat(viewModel.paymentSelection).isNull()
        assertThat(viewModel.state).isNull()
    }

    @Test
    fun `successful payment should not clear viewmodel state if specified`() = confirmationTest {
        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess()

        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            mock(),
            mock()
        )

        viewModel.paymentSelection = paymentSelection

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                deferredIntentConfirmationType = null,
                completedFullPaymentFlow = false,
            )
        )

        assertThat(viewModel.paymentSelection).isEqualTo(paymentSelection)
        assertThat(viewModel.state).isNotNull()
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
    fun `On fail due to invalid deferred intent usage, should report with expected integration error`() =
        confirmationTest {
            val eventReporter = FakeEventReporter()
            val flowController = createFlowController(
                eventReporter = eventReporter,
            ).apply {
                configureWithIntentConfiguration(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5000,
                            currency = "USD",
                        ),
                    ),
                    callback = { _, _ -> }
                )
            }

            val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION

            flowController.onPaymentOptionResult(
                PaymentOptionsActivityResult.Succeeded(
                    paymentSelection = selection,
                    linkAccountInfo = LinkAccountUpdate.Value(null)
                )
            )

            flowController.confirm()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                PaymentMethodConfirmationOption.New(
                    createParams = selection.paymentMethodCreateParams,
                    optionsParams = selection.paymentMethodOptionsParams,
                    extraParams = selection.paymentMethodExtraParams,
                    shouldSave = selection.customerRequestedSave == PaymentSelection
                        .CustomerRequestedSave.RequestReuse,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = InvalidDeferredIntentUsageException(),
                    message = "An error occurred!".resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            )

            val error = eventReporter.paymentFailureCalls.awaitItem().error

            assertThat(error.analyticsValue).isEqualTo("invalidDeferredIntentUsage")
            assertThat(error.cause).isInstanceOf(InvalidDeferredIntentUsageException::class.java)
        }

    @Test
    fun `Sends correct event for failed Google Pay payment`() = confirmationTest {
        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        val googlePayConfig = config.googlePay!!

        flowController.configureExpectingSuccess(
            configuration = config,
        )

        viewModel.paymentSelection = PaymentSelection.GooglePay

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            GooglePayConfirmationOption(
                config = GooglePayConfirmationOption.Config(
                    environment = googlePayConfig.environment,
                    customLabel = googlePayConfig.label,
                    customAmount = googlePayConfig.amount,
                    merchantCountryCode = googlePayConfig.countryCode,
                    merchantCurrencyCode = googlePayConfig.currencyCode,
                    merchantName = config.merchantDisplayName,
                    billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
                    cardBrandFilter = PaymentSheetCardBrandFilter(config.cardBrandAcceptance),
                )
            )
        )

        val errorCode = GooglePayPaymentMethodLauncher.INTERNAL_ERROR

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = InvalidDeferredIntentUsageException(),
                message = "An error occurred!".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(errorCode),
            )
        )

        verify(eventReporter).onPaymentFailure(
            paymentSelection = isA<PaymentSelection.GooglePay>(),
            error = eq(PaymentSheetConfirmationError.GooglePay(errorCode)),
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
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods),
            paymentSelection = PaymentSelection.Saved(paymentMethods.first()),
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        val paymentOption = flowController.getPaymentOption()
        assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption?.label).isEqualTo("···· $last4")
        assertThat(paymentOption?.paymentMethodType).isEqualTo("card")
    }

    @Test
    fun `getPaymentOption() for new customer without saved payment methods returns null`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(5)
        val last4 = paymentMethods.first().card?.last4.orEmpty()

        // Initially configure for a customer with saved payment methods
        val paymentSheetLoader = FakePaymentElementLoader(
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods),
            paymentSelection = PaymentSelection.Saved(paymentMethods.first()),
        )

        val flowController = createFlowController(paymentSheetLoader)

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        val paymentOption = flowController.getPaymentOption()
        assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption?.label).isEqualTo("···· $last4")
        assertThat(paymentOption?.paymentMethodType).isEqualTo("card")

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
            paymentElementLoader = FakePaymentElementLoader(shouldFail = true)
        ).configureExpectingError()
    }

    @Test
    fun `presentPaymentOptions() after successful init should launch with expected args`() = runTest {
        val flowController = createFlowController(linkState = null)

        flowController.configureExpectingSuccess()

        flowController.presentPaymentOptions()

        val expectedArgs = PaymentOptionContract.Args(
            state = PaymentSheetState.Full(
                customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                config = PaymentSheet.Configuration("com.stripe.android.paymentsheet.test").asCommonConfiguration(),
                paymentSelection = null,
                validationError = null,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(allowsDelayedPaymentMethods = false),
            ),
            configuration = PaymentSheet.Configuration("com.stripe.android.paymentsheet.test"),
            enableLogging = ENABLE_LOGGING,
            productUsage = PRODUCT_USAGE,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            paymentElementCallbackIdentifier = FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER,
            walletsToShow = WalletType.entries,
        )

        verify(paymentOptionActivityLauncher).launch(eq(expectedArgs), anyOrNull())
    }

    @Test
    fun `presentPaymentOptions() without successful init should fail`() {
        val flowController = createFlowController()

        verifyNoInteractions(paymentResultCallback)
        flowController.presentPaymentOptions()
        val resultCaptor = argumentCaptor<PaymentSheetResult.Failed>()
        verify(paymentResultCallback).onPaymentSheetResult(resultCaptor.capture())

        assertThat(resultCaptor.firstValue.error).hasMessageThat()
            .startsWith("FlowController must be successfully initialized")
    }

    @Test
    fun `presentPaymentOptions() with activity destroyed should fail`() = runTest {
        val flowController = createFlowController()
        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        )
        lifecycleOwner.currentState = Lifecycle.State.DESTROYED
        whenever(paymentOptionActivityLauncher.launch(any(), any())).thenThrow(IllegalStateException("Boom"))
        verifyNoInteractions(paymentResultCallback)

        flowController.presentPaymentOptions()
        val resultCaptor = argumentCaptor<PaymentSheetResult.Failed>()
        verify(paymentResultCallback).onPaymentSheetResult(resultCaptor.capture())

        assertThat(resultCaptor.firstValue.error).hasMessageThat()
            .isEqualTo("The host activity is not in a valid state (DESTROYED).")
    }

    @Test
    fun `presentPaymentOptions shows Link picker when a Link payment method is already selected`() = runTest {
        linkGate.setShowRuxInFlowController(true)

        val verifiedLinkAccount = TestFactory.LINK_ACCOUNT
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link(
                selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = null,
                    billingPhone = null
                )
            )
        )
        linkAccountHolder.set(LinkAccountUpdate.Value(verifiedLinkAccount))

        flowController.configureExpectingSuccess()

        flowController.presentPaymentOptions()

        verify(flowControllerLinkPaymentLauncher).present(
            configuration = any(),
            linkAccountInfo = anyOrNull(),
            launchMode = any(),
            linkExpressMode = any()
        )

        verify(paymentOptionActivityLauncher, never()).launch(any(), anyOrNull())
    }

    @Test
    fun `presentPaymentOptions should not launch Link 2FA after dismissing it once`() = runTest {
        linkGate.setShowRuxInFlowController(true)

        // Create a verificationStartedAccount with VerificationStarted status
        val session = CONSUMER_SESSION.copy(
            verificationSessions = listOf(VERIFICATION_STARTED_SESSION)
        )
        val verificationStartedAccount = LinkAccount(consumerSession = session)

        // Create flow controller with Link payment method
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link(
                selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = null,
                    billingPhone = null
                )
            )
        )

        // Set the account holder with VerificationStarted status
        linkAccountHolder.set(LinkAccountUpdate.Value(verificationStartedAccount))

        flowController.configureExpectingSuccess()

        // First call to present payment options
        flowController.presentPaymentOptions()

        // Verify Link launcher was called for the first time
        verify(flowControllerLinkPaymentLauncher).present(
            configuration = any(),
            linkAccountInfo = anyOrNull(),
            launchMode = any(),
            linkExpressMode = any()
        )

        // Simulate user dismissing 2FA with back press
        flowController.onLinkResultFromFlowController(
            LinkActivityResult.Canceled(
                reason = Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.Value(verificationStartedAccount)
            )
        )

        // Reset the mock to clear previous invocations
        reset(flowControllerLinkPaymentLauncher)

        // Try to present payment options again
        flowController.presentPaymentOptions()

        // Verify Link launcher was NOT called the second time
        verify(flowControllerLinkPaymentLauncher, never()).present(
            configuration = any(),
            linkAccountInfo = anyOrNull(),
            linkExpressMode = any(),
            launchMode = any()
        )

        // Verify payment option launcher was called instead
        verify(paymentOptionActivityLauncher).launch(any(), anyOrNull())
    }

    @Test
    fun `onLinkResultFromFlowController with logout should fallback to default saved payment method`() = runTest {
        val savedPaymentMethods = PaymentMethodFixtures.createCards(3)
        val defaultPaymentMethodId = savedPaymentMethods.first().id

        val customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = savedPaymentMethods,
            defaultPaymentMethodId = defaultPaymentMethodId
        )

        val flowController = createFlowController(
            customer = customer,
            paymentSelection = PaymentSelection.Link(
                selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = null,
                    billingPhone = null
                )
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    // Enable default payment method feature
                    attachDefaultsToPaymentMethod = true
                )
            )
        )

        // Verify initial state - should have Link selected (getPaymentOption() returns null for Link)
        // Link selections don't have payment options until configured

        // Simulate Link logout through the public API
        flowController.onLinkResultFromFlowController(
            LinkActivityResult.Canceled(
                reason = Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.Value(null)
            )
        )

        // Should fall back to default saved payment method - verify using getPaymentOption()
        val paymentOption = flowController.getPaymentOption()
        assertThat(paymentOption).isNotNull()
        assertThat(paymentOption?.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption?.label).isEqualTo("···· ${savedPaymentMethods.first().card?.last4}")

        // Verify callback was invoked with the fallback payment option
        verify(paymentOptionResultCallback).onPaymentOptionResult(
            argThat { result ->
                result.paymentOption != null &&
                    result.paymentOption?.paymentMethodType == "card" &&
                    result.didCancel // should be true since canceled = true for logout
            }
        )
    }

    @Test
    fun `onLinkResultFromFlowController with logout and no default should fallback to first saved payment method`() =
        runTest {
            val savedPaymentMethods = PaymentMethodFixtures.createCards(3)

            val customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = savedPaymentMethods,
                defaultPaymentMethodId = null // No default set
            )

            val flowController = createFlowController(
                customer = customer,
                paymentSelection = PaymentSelection.Link(
                    selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                        details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                        collectedCvc = null,
                        billingPhone = null
                    )
                )
            )

            flowController.configureExpectingSuccess()

            // Verify initial state - should have Link selected (getPaymentOption() returns null for Link)
            // Link selections don't have payment options until configured

            // Simulate Link logout through the public API
            flowController.onLinkResultFromFlowController(
                LinkActivityResult.Canceled(
                    reason = Reason.LoggedOut,
                    linkAccountUpdate = LinkAccountUpdate.Value(null)
                )
            )

            // Should fall back to first saved payment method (most recently used) - verify using getPaymentOption()
            val paymentOption = flowController.getPaymentOption()
            assertThat(paymentOption).isNotNull()
            assertThat(paymentOption?.paymentMethodType).isEqualTo("card")
            assertThat(paymentOption?.label).isEqualTo("···· ${savedPaymentMethods.first().card?.last4}")

            // Verify callback was invoked
            verify(paymentOptionResultCallback).onPaymentOptionResult(
                argThat { result ->
                    result.paymentOption != null &&
                        result.paymentOption?.paymentMethodType == "card" &&
                        result.didCancel // should be true since canceled = true for logout
                }
            )
        }

    @Test
    fun `onPaymentOptionResult() with saved payment method selection result should invoke callback with payment option`() =
        runTest {
            val flowController = createFlowController()

            flowController.configureExpectingSuccess(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            )

            flowController.onPaymentOptionResult(
                PaymentOptionsActivityResult.Succeeded(
                    paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                    linkAccountInfo = LinkAccountUpdate.Value(null)
                )
            )

            verify(paymentOptionResultCallback).onPaymentOptionResult(
                argThat {
                    paymentOption?.drawableResourceId == R.drawable.stripe_ic_paymentsheet_card_visa_ref &&
                        paymentOption?.label == "···· 4242" &&
                        !didCancel
                }
            )
            val paymentOption = flowController.getPaymentOption()
            assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
            assertThat(paymentOption?.label).isEqualTo("···· 4242")
            assertThat(paymentOption?.paymentMethodType).isEqualTo("card")
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

        verify(paymentOptionResultCallback).onPaymentOptionResult(
            argThat {
                paymentOption == null && didCancel
            }
        )
    }

    @Test
    fun `onPaymentOptionResult() adds payment method which is added on next open`() = runTest {
        // Create a default flow controller with the paymentMethods initialized with cards.
        val initialPaymentMethods = PaymentMethodFixtures.createCards(5)
        val flowController = createFlowController(
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(paymentMethods = initialPaymentMethods),
            paymentSelection = PaymentSelection.Saved(initialPaymentMethods.first())
        )
        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        // Add a saved card payment method so that we can make sure it is added when we open
        // up the payment option launcher
        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                SAVE_NEW_CARD_SELECTION,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )
        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere {
                // Make sure that paymentMethods contains the new added payment methods and the initial payment methods.
                it.state.customer?.paymentMethods == initialPaymentMethods
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
            PaymentOptionsActivityResult.Canceled(
                mostRecentError = null,
                paymentSelection = null,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        verify(paymentOptionResultCallback).onPaymentOptionResult(
            argThat {
                paymentOption == null && didCancel
            }
        )
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
            PaymentOptionsActivityResult.Canceled(
                mostRecentError = null,
                paymentSelection = PaymentSelection.GooglePay,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        verify(paymentOptionResultCallback).onPaymentOptionResult(
            argThat {
                paymentOption?.drawableResourceId == R.drawable.stripe_google_pay_mark &&
                    paymentOption?.label == "Google Pay" &&
                    didCancel
            }
        )
        val paymentOption = flowController.getPaymentOption()
        assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption?.label).isEqualTo("Google Pay")
        assertThat(paymentOption?.paymentMethodType).isEqualTo("google_pay")
    }

    @Test
    fun `confirmPayment() without paymentSelection should not call confirmation handler`() = confirmationTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.confirm()
    }

    @Test
    fun `confirmPaymentSelection() with new card payment method should start confirmation`() = confirmationTest {
        val flowController = createFlowController(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        flowController.configureExpectingSuccess()

        val initialSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )

        flowController.confirmPaymentSelection(
            paymentSelection = NEW_CARD_PAYMENT_SELECTION,
            state = PaymentSheetState.Full(
                PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                    paymentMethods = PAYMENT_METHODS
                ),
                paymentSelection = initialSelection,
                validationError = null,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            ),
            appearance = PaymentSheetFixtures.CONFIG_CUSTOMER.appearance,
            initializationMode = INITIALIZATION_MODE,
        )

        verifyPaymentSelection(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
        )
    }

    @Test
    fun `confirmPaymentSelection() with generic payment method should start confirmation`() = confirmationTest {
        val flowController = createFlowController(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        )

        flowController.configureExpectingSuccess()

        val initialSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )

        flowController.confirmPaymentSelection(
            paymentSelection = GENERIC_PAYMENT_SELECTION,
            state = PaymentSheetState.Full(
                PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                    paymentMethods = PAYMENT_METHODS
                ),
                paymentSelection = initialSelection,
                validationError = null,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            ),
            appearance = PaymentSheetFixtures.CONFIG_CUSTOMER.appearance,
            initializationMode = INITIALIZATION_MODE,
        )

        verifyPaymentSelection(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            GENERIC_PAYMENT_SELECTION.paymentMethodCreateParams
        )
    }

    @Test
    fun `confirmPaymentSelection() with us_bank_account payment method should start confirmation`() =
        confirmationTest {
            val flowController = createFlowController(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            )

            flowController.configureExpectingSuccess()

            val paymentSelection = GENERIC_PAYMENT_SELECTION.copy(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT,
                paymentMethodOptionsParams = PaymentMethodOptionsParams.USBankAccount(),
            )

            val initialSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )

            flowController.confirmPaymentSelection(
                paymentSelection = paymentSelection,
                state = PaymentSheetState.Full(
                    PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                        paymentMethods = PAYMENT_METHODS
                    ),
                    paymentSelection = initialSelection,
                    validationError = null,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                ),
                appearance = PaymentSheetFixtures.CONFIG_CUSTOMER.appearance,
                initializationMode = INITIALIZATION_MODE,
            )

            verifyPaymentSelection(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                paymentSelection.paymentMethodCreateParams,
                PaymentMethodOptionsParams.USBankAccount()
            )
        }

    @Test
    fun `confirmPaymentSelection() with null payment selection, should return failure`() = runTest {
        val errorReporter = FakeErrorReporter()

        val flowController = createFlowController(
            paymentSelection = null,
            errorReporter = errorReporter,
        )

        flowController.configureExpectingSuccess()

        flowController.confirmPaymentSelection(
            paymentSelection = null,
            state = PAYMENT_SHEET_STATE_FULL,
            appearance = PaymentSheetFixtures.CONFIG_CUSTOMER.appearance,
            initializationMode = INITIALIZATION_MODE,
        )

        assertThat(errorReporter.getLoggedErrors()).isEmpty()

        verify(paymentResultCallback).onPaymentSheetResult(isA<PaymentSheetResult.Failed>())
    }

    @Test
    fun `confirm() with invalid payment selection, should report event and return failure`() = runTest {
        val errorReporter = FakeErrorReporter()

        val flowController = createFlowController(
            paymentSelection = null,
            errorReporter = errorReporter,
        )

        flowController.configureExpectingSuccess()

        flowController.confirmPaymentSelection(
            paymentSelection = PaymentSelection.Link(),
            state = PAYMENT_SHEET_STATE_FULL,
            appearance = PaymentSheetFixtures.CONFIG_CUSTOMER.appearance,
            initializationMode = INITIALIZATION_MODE,
        )

        assertThat(errorReporter.getLoggedErrors()).contains(
            "unexpected_error.flow_controller.invalid_payment_selection"
        )

        verify(paymentResultCallback).onPaymentSheetResult(isA<PaymentSheetResult.Failed>())
    }

    @Test
    fun `confirmPaymentSelection() with link payment method should launch LinkPaymentLauncher`() = confirmationTest {
        val flowController = createFlowController(
            paymentSelection = PaymentSelection.Link(),
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes.plus("link")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            LinkConfirmationOption(
                linkExpressMode = LinkExpressMode.DISABLED,
                configuration = TestFactory.LINK_CONFIGURATION,
            )
        )
    }

    @Test
    fun `confirmPaymentSelection() with LinkInline and user not signed in should start confirmation`() =
        confirmationTest {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures
                    .PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes
                    .plus("link")
            )

            val flowController = createFlowController(
                paymentSelection = PaymentSelection.Link(),
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = LinkSignupMode.AlongsideSaveForFutureUse,
                ),
                stripeIntent = intent,
            )

            flowController.configureExpectingSuccess(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            val paymentSelection = PaymentMethodFixtures.LINK_INLINE_PAYMENT_SELECTION

            flowController.onPaymentOptionResult(
                PaymentOptionsActivityResult.Succeeded(
                    paymentSelection,
                    linkAccountInfo = LinkAccountUpdate.Value(null)
                )
            )

            flowController.confirm()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                LinkInlineSignupConfirmationOption(
                    createParams = paymentSelection.paymentMethodCreateParams,
                    optionsParams = paymentSelection.paymentMethodOptionsParams,
                    extraParams = paymentSelection.paymentMethodExtraParams,
                    saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest,
                    linkConfiguration = TestFactory.LINK_CONFIGURATION,
                    userInput = paymentSelection.input,
                )
            )

            assertThat(arguments.intent).isEqualTo(intent)
        }

    @Test
    fun `confirmPaymentSelection() with Link and shipping should have shipping details in config`() =
        confirmationTest {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures
                    .PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes
                    .plus("link")
            )

            val linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
                shippingDetails = AddressDetails(
                    name = "Test",
                    address = PaymentSheet.Address(),
                )
            )

            val flowController = createFlowController(
                paymentSelection = PaymentSelection.Link(),
                linkState = LinkState(
                    configuration = linkConfiguration,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
                stripeIntent = intent,
            )

            flowController.configureExpectingSuccess(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            val paymentSelection = PaymentMethodFixtures.LINK_INLINE_PAYMENT_SELECTION

            flowController.onPaymentOptionResult(
                PaymentOptionsActivityResult.Succeeded(
                    paymentSelection,
                    linkAccountInfo = LinkAccountUpdate.Value(null)
                )
            )

            flowController.confirm()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                LinkInlineSignupConfirmationOption(
                    createParams = paymentSelection.paymentMethodCreateParams,
                    optionsParams = paymentSelection.paymentMethodOptionsParams,
                    extraParams = paymentSelection.paymentMethodExtraParams,
                    userInput = paymentSelection.input,
                    linkConfiguration = linkConfiguration,
                    saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest,
                )
            )
            assertThat(arguments.intent).isEqualTo(intent)
        }

    @Test
    fun `confirmPaymentSelection() with Link and no shipping should not have shipping details in confirm params`() =
        confirmationTest {
            val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures
                    .PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes
                    .plus("link")
            )

            val linkConfig = TestFactory.LINK_CONFIGURATION.copy(
                shippingDetails = null,
            )

            val flowController = createFlowController(
                paymentSelection = PaymentSelection.Link(),
                linkState = LinkState(
                    configuration = linkConfig,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
                stripeIntent = intent,
            )

            flowController.configureExpectingSuccess(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            )

            val paymentSelection = PaymentMethodFixtures.LINK_INLINE_PAYMENT_SELECTION

            flowController.onPaymentOptionResult(
                PaymentOptionsActivityResult.Succeeded(
                    paymentSelection = paymentSelection,
                    linkAccountInfo = LinkAccountUpdate.Value(null)
                )
            )

            flowController.confirm()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                LinkInlineSignupConfirmationOption(
                    createParams = paymentSelection.paymentMethodCreateParams,
                    optionsParams = paymentSelection.paymentMethodOptionsParams,
                    extraParams = paymentSelection.paymentMethodExtraParams,
                    saveOption = LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest,
                    linkConfiguration = linkConfig,
                    userInput = paymentSelection.input,
                )
            )
            assertThat(arguments.intent).isEqualTo(intent)
        }

    @Test
    fun `confirm() with default sepa saved payment method should show sepa mandate`() = confirmationTest {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val flowController = createFlowController(
            paymentSelection = paymentSelection,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes.plus("sepa_debit")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.newBuilder()
                .allowsDelayedPaymentMethods(true)
                .build()
        )

        flowController.confirm()

        verify(sepaMandateActivityLauncher).launch(any())

        flowController.onSepaMandateResult(SepaMandateResult.Acknowledged)

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                optionsParams = null,
            )
        )
    }

    @Test
    fun `confirm() with default sepa saved payment method should cancel after show sepa mandate`() = confirmationTest {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val flowController = createFlowController(
            paymentSelection = paymentSelection,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes.plus("sepa_debit")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.newBuilder()
                .allowsDelayedPaymentMethods(true)
                .build()
        )

        flowController.confirm()

        verify(sepaMandateActivityLauncher).launch(any())

        flowController.onSepaMandateResult(SepaMandateResult.Canceled)
    }

    @Test
    fun `confirm() selecting sepa saved payment method`() = confirmationTest {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        val flowController = createFlowController(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
                    .paymentMethodTypes.plus("sepa_debit")
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.newBuilder()
                .allowsDelayedPaymentMethods(true)
                .build()
        )

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        verify(sepaMandateActivityLauncher, never()).launch(any())

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                optionsParams = null,
            )
        )
    }

    private suspend fun FakeConfirmationHandler.Scenario.verifyPaymentSelection(
        intent: StripeIntent,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        expectedPaymentMethodOptions: PaymentMethodOptionsParams? = null
    ) {
        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.New(
                createParams = paymentMethodCreateParams,
                optionsParams = expectedPaymentMethodOptions,
                extraParams = null,
                shouldSave = false,
            )
        )
        assertThat(arguments.intent).isEqualTo(intent)
    }

    @Test
    fun `confirmPayment() with GooglePay should start confirmation`() = confirmationTest {
        val flowController = createFlowController()

        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        val googlePayConfig = config.googlePay!!

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )
        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                PaymentSelection.GooglePay,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )
        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            GooglePayConfirmationOption(
                config = GooglePayConfirmationOption.Config(
                    environment = googlePayConfig.environment,
                    customLabel = googlePayConfig.label,
                    customAmount = googlePayConfig.amount,
                    merchantCountryCode = googlePayConfig.countryCode,
                    merchantCurrencyCode = googlePayConfig.currencyCode,
                    merchantName = config.merchantDisplayName,
                    billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
                    cardBrandFilter = PaymentSheetCardBrandFilter(config.cardBrandAcceptance),
                )
            )
        )
    }

    @Test
    fun `When confirm canceled should invoke callback with canceled result`() = confirmationTest {
        verifyNoInteractions(eventReporter)

        val viewModel = createViewModel()
        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )

        viewModel.paymentSelection = PaymentSelection.GooglePay

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Canceled(
                action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
            )
        )

        verify(paymentResultCallback).onPaymentSheetResult(
            PaymentSheetResult.Canceled
        )
    }

    @Test
    fun `On payment error, should report stripe failure`() = confirmationTest {
        val eventReporter = FakeEventReporter()
        val flowController = createFlowController(
            eventReporter = eventReporter,
        ).apply {
            configureExpectingSuccess()
        }

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection = GENERIC_PAYMENT_SELECTION,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )
        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.New(
                createParams = GENERIC_PAYMENT_SELECTION.paymentMethodCreateParams,
                optionsParams = GENERIC_PAYMENT_SELECTION.paymentMethodOptionsParams,
                extraParams = GENERIC_PAYMENT_SELECTION.paymentMethodExtraParams,
                shouldSave = GENERIC_PAYMENT_SELECTION.customerRequestedSave ==
                    PaymentSelection.CustomerRequestedSave.RequestReuse,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("Failed!"),
                message = "Failed!".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        val failureCall = eventReporter.paymentFailureCalls.awaitItem()

        assertThat(failureCall.error).isInstanceOf(PaymentSheetConfirmationError.Stripe::class.java)
    }

    @Test
    fun `confirmPayment() with Link should launch Link`() = confirmationTest {
        val flowController = createFlowController(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            )
        )

        flowController.configureExpectingSuccess(
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )
        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult
                .Succeeded(PaymentSelection.Link(), linkAccountInfo = LinkAccountUpdate.Value(null))
        )
        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            LinkConfirmationOption(
                linkExpressMode = LinkExpressMode.DISABLED,
                configuration = TestFactory.LINK_CONFIGURATION,
            )
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
    fun `onPaymentResult with Link payment successful should logout when merchant is not verified`() =
        runTest {
            val linkHandler = mock<LinkHandler>()
            val viewModel = createViewModel()

            // Configure with Link payment selection and non-verified merchant (useAttestationEndpointsForLink = false)
            val linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
                useAttestationEndpointsForLink = false
            )
            val linkState = LinkState(
                configuration = linkConfiguration,
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null,
            )
            val flowController = createFlowController(
                linkHandler = linkHandler,
                viewModel = viewModel,
                linkState = linkState,
                paymentSelection = PaymentSelection.Link()
            )

            flowController.configureExpectingSuccess()

            // Call onPaymentResult with completed payment
            flowController.onPaymentResult(PaymentResult.Completed)

            // Verify that Link logout was called since merchant is not verified
            verify(linkHandler).logOut()
        }

    @Test
    fun `onPaymentResult with Link payment successful should not logout when merchant is verified`() =
        runTest {
            val linkHandler = mock<LinkHandler>()
            val viewModel = createViewModel()

            // Configure with Link payment selection and verified merchant (useAttestationEndpointsForLink = true)
            val linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
                useAttestationEndpointsForLink = true
            )
            val linkState = LinkState(
                configuration = linkConfiguration,
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null,
            )
            val flowController = createFlowController(
                linkHandler = linkHandler,
                viewModel = viewModel,
                linkState = linkState,
                paymentSelection = PaymentSelection.Link()
            )

            flowController.configureExpectingSuccess()

            // Call onPaymentResult with completed payment
            flowController.onPaymentResult(PaymentResult.Completed)

            // Verify that Link logout was NOT called since merchant is verified
            verify(linkHandler, never()).logOut()
        }

    @Test
    fun `Remembers previous new payment selection when presenting payment options again`() = runTest {
        val flowController = createFlowController()

        flowController.configureExpectingSuccess()

        val previousPaymentSelection = NEW_CARD_PAYMENT_SELECTION

        flowController.onPaymentOptionResult(
            result = PaymentOptionsActivityResult.Succeeded(
                previousPaymentSelection,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            ),
        )

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere { it.state.paymentSelection == previousPaymentSelection },
            anyOrNull(),
        )
    }

    @Test
    fun `On wallet buttons rendered and options launched, should show no wallets in options screen`() = runTest {
        val viewModel = createViewModel()

        viewModel.walletButtonsRendered = true

        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess()

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere { it.walletsToShow.isEmpty() },
            anyOrNull(),
        )
    }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `On wallet buttons rendered and options launched, should show only Link in options screen`() = runTest {
        val viewModel = createViewModel()

        viewModel.walletButtonsRendered = true

        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess(
            configuration = PaymentSheet.Configuration.Builder(
                merchantDisplayName = "Example, Inc."
            )
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration(
                        willDisplayExternally = true,
                        walletsToShow = listOf("google_pay", "shop_pay")
                    )
                )
                .build()
        )

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere {
                it.walletsToShow.size == 1 &&
                    it.walletsToShow.contains(WalletType.Link)
            },
            anyOrNull(),
        )
    }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `On wallet buttons rendered and options launched, should show only GPay in options screen`() = runTest {
        val viewModel = createViewModel()

        viewModel.walletButtonsRendered = true

        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess(
            configuration = PaymentSheet.Configuration.Builder(
                merchantDisplayName = "Example, Inc."
            )
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration(
                        willDisplayExternally = true,
                        walletsToShow = listOf("link", "shop_pay")
                    )
                )
                .build()
        )

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere {
                it.walletsToShow.size == 1 &&
                    it.walletsToShow.contains(WalletType.GooglePay)
            },
            anyOrNull(),
        )
    }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `On wallet buttons rendered and options launched, should show only Shop Pay in options screen`() = runTest {
        val viewModel = createViewModel()

        viewModel.walletButtonsRendered = true

        val flowController = createFlowController(viewModel = viewModel)

        flowController.configureExpectingSuccess(
            configuration = PaymentSheet.Configuration.Builder(
                merchantDisplayName = "Example, Inc."
            )
                .googlePay(
                    PaymentSheet.GooglePayConfiguration(
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                .walletButtons(
                    PaymentSheet.WalletButtonsConfiguration(
                        willDisplayExternally = true,
                        walletsToShow = listOf("link", "google_pay")
                    )
                )
                .build()
        )

        flowController.presentPaymentOptions()

        verify(paymentOptionActivityLauncher).launch(
            argWhere {
                it.walletsToShow.size == 1 &&
                    it.walletsToShow.contains(WalletType.ShopPay)
            },
            anyOrNull(),
        )
    }

    @Test
    fun `Calls confirmation handler with deferred intents`() = confirmationTest {
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 12345,
                currency = "usd"
            )
        )
        val flowController = createAndConfigureFlowControllerForDeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 12345,
                    currency = "usd"
                )
            )
        )

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            )
        )
        assertThat(arguments.initializationMode)
            .isEqualTo(PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration))
    }

    @Test
    fun `Completes if confirmation handler succeeds with deferred intents`() = confirmationTest {
        val flowController = createAndConfigureFlowControllerForDeferredIntent(
            paymentIntent = PaymentIntentFixtures.PI_SUCCEEDED,
        )

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        verify(paymentResultCallback).onPaymentSheetResult(PaymentSheetResult.Completed)
    }

    @Test
    fun `Returns failure if confirmation handler returns a failure with deferred intents`() = confirmationTest {
        val flowController = createAndConfigureFlowControllerForDeferredIntent()

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = Exception("something went wrong"),
                message = "something went wrong".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
            )
        )

        verify(paymentResultCallback).onPaymentSheetResult(
            argWhere {
                (it as PaymentSheetResult.Failed).error.message == "something went wrong"
            }
        )
    }

    @Test
    fun `Returns failure if attempting to confirm while configure calls is in-flight`() = runTest {
        val mockLoader = RelayingPaymentElementLoader()
        val flowController = createFlowController(paymentElementLoader = mockLoader)

        mockLoader.enqueueSuccess()

        flowController.configureExpectingSuccess()

        // Simulate that the user has selected a payment method
        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult
                .Succeeded(PaymentSelection.GooglePay, linkAccountInfo = LinkAccountUpdate.Value(null))
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
        val mockLoader = RelayingPaymentElementLoader()
        val flowController = createFlowController(paymentElementLoader = mockLoader)

        mockLoader.enqueueSuccess()

        flowController.configureExpectingSuccess()

        // Simulate that the user has selected a payment method
        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult
                .Succeeded(PaymentSelection.GooglePay, linkAccountInfo = LinkAccountUpdate.Value(null))
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
        val mockLoader = RelayingPaymentElementLoader()
        val flowController = createFlowController(paymentElementLoader = mockLoader)

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
        val mockLoader = RelayingPaymentElementLoader()
        val flowController = createFlowController(paymentElementLoader = mockLoader)

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

    @Test
    fun `Sends no deferred_intent_confirmation_type for non-deferred intent confirmation`() = confirmationTest {
        val flowController = createFlowController().apply {
            configureExpectingSuccess()
        }

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(savedSelection, linkAccountInfo = LinkAccountUpdate.Value(null))
        )
        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            deferredIntentConfirmationType = isNull(),
        )
    }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for client-side confirmation of deferred intent`() =
        confirmationTest {
            val flowController = createAndConfigureFlowControllerForDeferredIntent()

            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val savedSelection = PaymentSelection.Saved(paymentMethod)

            flowController.onPaymentOptionResult(
                PaymentOptionsActivityResult.Succeeded(savedSelection, linkAccountInfo = LinkAccountUpdate.Value(null))
            )
            flowController.confirm()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PaymentIntentFixtures.PI_SUCCEEDED,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
                )
            )

            verify(eventReporter).onPaymentSuccess(
                paymentSelection = eq(savedSelection),
                deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Client),
            )
        }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for server-side confirmation of deferred intent`() =
        confirmationTest {
            val flowController = createAndConfigureFlowControllerForDeferredIntent()

            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            val savedSelection = PaymentSelection.Saved(paymentMethod)

            flowController.onPaymentOptionResult(
                PaymentOptionsActivityResult.Succeeded(savedSelection, linkAccountInfo = LinkAccountUpdate.Value(null))
            )
            flowController.confirm()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.Saved>()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PaymentIntentFixtures.PI_SUCCEEDED,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                )
            )

            verify(eventReporter).onPaymentSuccess(
                paymentSelection = eq(savedSelection),
                deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Server),
            )
        }

    @Test
    fun `Confirms Google Pay with custom label and amount if provided`() = confirmationTest {
        val expectedLabel = "My custom label"
        val expectedAmount = 1099L

        val flowController = createFlowController()

        val config = PaymentSheet.Configuration(
            merchantDisplayName = "My merchant",
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "CA",
                currencyCode = "CAD",
                amount = expectedAmount,
                label = expectedLabel,
            )
        )

        flowController.configureExpectingSuccess(
            clientSecret = PaymentSheetFixtures.CLIENT_SECRET,
            configuration = config,
        )

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult
                .Succeeded(
                    paymentSelection = PaymentSelection.GooglePay,
                    linkAccountInfo = LinkAccountUpdate.Value(null)
                )
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            GooglePayConfirmationOption(
                config = GooglePayConfirmationOption.Config(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    merchantCountryCode = "CA",
                    merchantCurrencyCode = "CAD",
                    customAmount = expectedAmount,
                    customLabel = expectedLabel,
                    merchantName = "My merchant",
                    billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
                    cardBrandFilter = PaymentSheetCardBrandFilter(config.cardBrandAcceptance),
                )
            )
        )
    }

    @Test
    fun `Confirms Bacs with correct confirmation option`() = confirmationTest {
        val flowController = createFlowController()

        val appearance = PaymentSheet.Appearance.Builder()
            .colorsDark(PaymentSheet.Colors.defaultLight)
            .build()

        flowController.configureExpectingSuccess(
            clientSecret = PaymentSheetFixtures.SETUP_CLIENT_SECRET,
            configuration = PaymentSheet.Configuration.Builder(
                merchantDisplayName = "My merchant"
            )
                .appearance(appearance)
                .build()
        )

        val selection = createBacsPaymentSelection()

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection = selection,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            BacsConfirmationOption(
                createParams = selection.paymentMethodCreateParams,
                optionsParams = selection.paymentMethodOptionsParams,
            )
        )
        assertThat(arguments.appearance).isEqualTo(appearance)
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
    fun `On complete internal payment result in PI mode & should not reuse, should not save payment selection`() =
        selectionSavedTest(shouldSave = false) { flowController ->
            flowController.configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_12345"
            ) { _, _ -> }
        }

    @Test
    fun `On complete internal payment result in SI mode, should save payment selection`() =
        selectionSavedTest { flowController ->
            flowController.configureWithSetupIntent(
                setupIntentClientSecret = "si_123456"
            ) { _, _ -> }
        }

    @Test
    fun `On complete internal payment result with intent config in PI mode, should not save payment selection`() =
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

    @Test
    fun `On complete internal payment result with intent config in PI+SFU mode, should save payment selection`() =
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

    @Test
    fun `On complete internal payment result with intent config in SI mode, should save payment selection`() =
        selectionSavedTest { flowController ->
            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "USD"
                    )
                )
            ) { _, _ -> }
        }

    @Test
    fun `On google pay intent result, should save payment selection as google_pay`() = confirmationTest {
        val flowController = createFlowController()

        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = "pi_12345",
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        ) { _, _ -> }

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                PaymentSelection.GooglePay,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )
        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        assertThat(
            prefsRepository.getSavedSelection(
                isGooglePayAvailable = true,
                isLinkAvailable = true
            )
        ).isEqualTo(
            SavedSelection.GooglePay
        )
    }

    @Test
    fun `Clears out CreateIntentCallback when lifecycle owner is destroyed`() {
        PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ ->
                error("I’m alive")
            }
            .build()

        createFlowController()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.createIntentCallback
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.createIntentCallback
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.createIntentCallback
        ).isNull()
    }

    @Test
    fun `Clears out externalPaymentMethodConfirmHandler when lifecycle owner is destroyed`() {
        PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .externalPaymentMethodConfirmHandler { _, _ ->
                error("I’m alive")
            }
            .build()

        createFlowController()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.externalPaymentMethodConfirmHandler
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.externalPaymentMethodConfirmHandler
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.externalPaymentMethodConfirmHandler
        ).isNull()
    }

    @Test
    fun `Clears out confirmCustomPaymentMethodCallback when lifecycle owner is destroyed`() {
        PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .confirmCustomPaymentMethodCallback { _, _ ->
                error("I’m alive")
            }
            .build()

        createFlowController()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.confirmCustomPaymentMethodCallback
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.confirmCustomPaymentMethodCallback
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(
            PaymentElementCallbackReferences[FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER]
                ?.confirmCustomPaymentMethodCallback
        ).isNull()
    }

    @Test
    fun `On external payment error, should report external payment method failure`() = confirmationTest {
        val eventReporter = FakeEventReporter()
        val flowController = createFlowController(
            eventReporter = eventReporter,
        ).apply {
            configureExpectingSuccess()
        }

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection = EXTERNAL_PAYMENT_SELECTION,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )
        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            ExternalPaymentMethodConfirmationOption(
                type = EXTERNAL_PAYMENT_SELECTION.type,
                billingDetails = EXTERNAL_PAYMENT_SELECTION.billingDetails,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = Exception("An error!"),
                message = "An error!".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.ExternalPaymentMethod,
            )
        )

        val failureCall = eventReporter.paymentFailureCalls.awaitItem()

        assertThat(failureCall.error).isEqualTo(PaymentSheetConfirmationError.ExternalPaymentMethod)
    }

    @Test
    fun `On confirm existing payment method & PI, should send expected params to handler`() = confirmationTest {
        val flowController = createFlowController()

        val shippingDetails = AddressDetails(
            name = "John Doe",
            phoneNumber = "11234567890",
            address = PaymentSheet.Address(
                line1 = "123 Apple Street",
                line2 = "Unit 47",
                city = "South San Francisco",
                state = "CA",
                country = "US",
                postalCode = "99899",
            )
        )

        flowController.configureWithPaymentIntent(
            paymentIntentClientSecret = "pi_123",
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant, Inc.",
                shippingDetails = shippingDetails,
            )
        ) { _, _ ->
            // Do nothing
        }

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection = PaymentSelection.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                        cvc = "505"
                    )
                ),
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = PaymentMethodOptionsParams.Card(
                    cvc = "505"
                ),
                originatedFromWallet = false,
            )
        )
        assertThat(arguments.shippingDetails).isEqualTo(shippingDetails)
    }

    @Test
    fun `On confirm new payment method & SI, should send expected params to interceptor`() = confirmationTest {
        val flowController = createFlowController()

        flowController.configureWithSetupIntent(
            setupIntentClientSecret = "si_123",
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Merchant, Inc.",
                shippingDetails = null
            )
        ) { _, _ ->
            // Do nothing
        }

        val card = PaymentMethodCreateParams.createCard(
            cardParams = CardParams(
                number = "4242424242424242",
                expMonth = 7,
                expYear = 2027
            )
        )

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection = PaymentSelection.New.Card(
                    paymentMethodCreateParams = card,
                    brand = CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                ),
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.New(
                createParams = card,
                optionsParams = null,
                extraParams = null,
                shouldSave = true,
            )
        )
        assertThat(arguments.shippingDetails).isNull()
    }

    private fun selectionSavedTest(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave =
            PaymentSelection.CustomerRequestedSave.NoRequest,
        shouldSave: Boolean = true,
        configure: (PaymentSheet.FlowController) -> Unit
    ) = confirmationTest {
        val paymentIntent = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD!!
        val flowController = createFlowController()

        configure(flowController)

        val createParams = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card()
        )

        val selection = PaymentSelection.New.Card(
            brand = CardBrand.Visa,
            customerRequestedSave = customerRequestedSave,
            paymentMethodCreateParams = createParams
        )

        flowController.onPaymentOptionResult(
            PaymentOptionsActivityResult.Succeeded(
                paymentSelection = selection,
                linkAccountInfo = LinkAccountUpdate.Value(null)
            )
        )

        flowController.confirm()

        assertThat(startTurbine.awaitItem().confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.New(
                shouldSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse,
                createParams = createParams,
                optionsParams = null,
                extraParams = null,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = paymentIntent,
                deferredIntentConfirmationType = null,
            )
        )

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

    private suspend fun FakeConfirmationHandler.Scenario.createAndConfigureFlowControllerForDeferredIntent(
        paymentIntent: PaymentIntent = PaymentIntentFixtures.PI_SUCCEEDED,
        intentConfiguration: PaymentSheet.IntentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 12345,
                currency = "usd"
            )
        ),
    ): DefaultFlowController {
        val deferredIntent = paymentIntent.copy(id = null, clientSecret = null)
        return createFlowController(
            stripeIntent = deferredIntent
        ).apply {
            configureWithIntentConfiguration(
                intentConfiguration = intentConfiguration,
                configuration = null,
                callback = { _, error ->
                    assertThat(error).isNull()
                },
            )
        }
    }

    private fun confirmationTest(
        block: suspend FakeConfirmationHandler.Scenario.(scope: TestScope) -> Unit,
    ) = runTest {
        FakeConfirmationHandler.test(
            hasReloadedFromProcessDeath = false,
            initialState = ConfirmationHandler.State.Idle,
        ) {
            block(this@runTest)
        }
    }

    private suspend fun FakeConfirmationHandler.Scenario.createFlowController(
        customer: CustomerState? = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        paymentSelection: PaymentSelection? = null,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        linkState: LinkState? = LinkState(
            configuration = mock(),
            loginState = LinkState.LoginState.LoggedIn,
            signupMode = null,
        ),
        viewModel: FlowControllerViewModel = createViewModel(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        eventReporter: EventReporter = this@DefaultFlowControllerTest.eventReporter,
    ): DefaultFlowController {
        return createFlowController(
            FakePaymentElementLoader(
                customer = customer,
                stripeIntent = stripeIntent,
                paymentSelection = paymentSelection,
                linkState = linkState,
            ),
            viewModel,
            errorReporter,
            eventReporter,
            handler,
        ).also {
            val registerCall = registerTurbine.awaitItem()

            assertThat(registerCall.activityResultCaller).isEqualTo(activityResultCaller)
            assertThat(registerCall.lifecycleOwner).isEqualTo(lifecycleOwner)
        }
    }

    private fun createFlowController(
        customer: CustomerState? = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        paymentSelection: PaymentSelection? = null,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        linkState: LinkState? = LinkState(
            configuration = mock(),
            loginState = LinkState.LoginState.LoggedIn,
            signupMode = null,
        ),
        viewModel: FlowControllerViewModel = createViewModel(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        eventReporter: EventReporter = this.eventReporter,
        confirmationHandler: ConfirmationHandler? = null,
        linkHandler: LinkHandler? = null,
    ): DefaultFlowController {
        return createFlowController(
            FakePaymentElementLoader(
                customer = customer,
                stripeIntent = stripeIntent,
                paymentSelection = paymentSelection,
                linkState = linkState,
            ),
            viewModel,
            errorReporter,
            eventReporter,
            confirmationHandler,
            linkHandler,
        )
    }

    private fun createFlowController(
        paymentElementLoader: PaymentElementLoader,
        viewModel: FlowControllerViewModel = createViewModel(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        eventReporter: EventReporter = this.eventReporter,
        confirmationHandler: ConfirmationHandler? = null,
        linkHandler: LinkHandler? = null,
    ): DefaultFlowController {
        return DefaultFlowController(
            viewModelScope = testScope,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            paymentOptionFactory = PaymentOptionFactory(
                iconLoader = PaymentSelection.IconLoader(
                    resources = context.resources,
                    imageLoader = StripeImageLoader(context),
                ),
                context = context,
            ),
            paymentOptionResultCallback = paymentOptionResultCallback,
            paymentResultCallback = paymentResultCallback,
            context = context,
            eventReporter = eventReporter,
            viewModel = viewModel,
            enableLogging = ENABLE_LOGGING,
            productUsage = PRODUCT_USAGE,
            prefsRepositoryFactory = { prefsRepository },
            configurationHandler = FlowControllerConfigurationHandler(
                paymentElementLoader = paymentElementLoader,
                uiContext = testDispatcher,
                eventReporter = eventReporter,
                viewModel = viewModel,
                paymentSelectionUpdater = { _, _, newState, _, _ -> newState.paymentSelection },
                isLiveModeProvider = { false },
            ),
            errorReporter = errorReporter,
            initializedViaCompose = false,
            linkHandler = linkHandler ?: mock(),
            paymentElementCallbackIdentifier = FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER,
            linkAccountHolder = linkAccountHolder,
            flowControllerLinkLauncher = flowControllerLinkPaymentLauncher,
            walletsButtonLinkLauncher = walletsButtonLinkPaymentLauncher,
            activityResultRegistryOwner = mock(),
            linkGateFactory = { linkGate },
            confirmationHandler = confirmationHandler ?: FakeConfirmationHandler(),
        )
    }

    private fun createViewModel(): FlowControllerViewModel {
        return FlowControllerViewModel(
            application = ApplicationProvider.getApplicationContext(),
            handle = SavedStateHandle(),
            statusBarColor = STATUS_BAR_COLOR,
            paymentElementCallbackIdentifier = FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER,
        )
    }

    private fun createBacsPaymentSelection(): PaymentSelection.New.GenericPaymentMethod {
        return PaymentSelection.New.GenericPaymentMethod(
            label = "Test".resolvableString,
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
            iconResource = R.drawable.stripe_ic_paymentsheet_card_visa_ref,
            label = "Bancontact".resolvableString,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.BANCONTACT,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )
        private val EXTERNAL_PAYMENT_SELECTION = PaymentSelection.ExternalPaymentMethod(
            type = "paypal",
            billingDetails = null,
            iconResource = 0,
            label = "Paypal".resolvableString,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )

        private val SAVE_NEW_CARD_SELECTION = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        private val PAYMENT_METHODS =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD) + PaymentMethodFixtures.createCards(5)

        private val PAYMENT_SHEET_STATE_FULL = PaymentSheetState.Full(
            PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = PAYMENT_METHODS
            ),
            paymentSelection = null,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        )

        private val INITIALIZATION_MODE = PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = "pi_123"
        )

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
