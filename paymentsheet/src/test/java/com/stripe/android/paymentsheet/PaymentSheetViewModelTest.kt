package com.stripe.android.paymentsheet

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.android.gms.common.api.Status
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentSheet.InitializationMode
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.UserErrorMessage
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.IntentConfirmationInterceptorTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration

@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val intentConfirmationInterceptorTestRule = IntentConfirmationInterceptorTestRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val lpmRepository = LpmRepository(
        arguments = LpmRepository.LpmRepositoryArguments(application.resources),
    ).apply {
        this.update(
            PaymentIntentFactory.create(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.USBankAccount.code,
                    PaymentMethod.Type.CashAppPay.code,
                    PaymentMethod.Type.Ideal.code,
                    PaymentMethod.Type.SepaDebit.code,
                    PaymentMethod.Type.Sofort.code,
                    PaymentMethod.Type.Affirm.code,
                    PaymentMethod.Type.AfterpayClearpay.code,
                )
            ),
            null
        )
    }

    private val prefsRepository = FakePrefsRepository()

    private val paymentLauncher = mock<StripePaymentLauncher> {
        on { authenticatorRegistry } doReturn mock()
    }
    private val paymentLauncherFactory = mock<StripePaymentLauncherAssistedFactory> {
        on { create(any(), any(), anyOrNull(), any()) } doReturn paymentLauncher
    }
    private val fakeIntentConfirmationInterceptor = FakeIntentConfirmationInterceptor()

    private val linkConfigurationCoordinator = mock<LinkConfigurationCoordinator> {
        on { getAccountStatusFlow(any()) } doReturn flowOf(AccountStatus.SignedOut)
    }

    @BeforeTest
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should fire analytics event`() {
        val beforeSessionId = AnalyticsRequestFactory.sessionId
        createViewModel()
        verify(eventReporter).onInit(
            configuration = eq(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY),
            isDecoupling = eq(false),
        )

        // Creating the view model should regenerate the analytics sessionId.
        assertThat(beforeSessionId).isNotEqualTo(AnalyticsRequestFactory.sessionId)
    }

    @Test
    fun `removePaymentMethod triggers async removal`() = runTest {
        val customerRepository = spy(FakeCustomerRepository())
        val viewModel = createViewModel(
            customerRepository = customerRepository
        )

        viewModel.removePaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        verify(customerRepository).detachPaymentMethod(
            any(),
            eq(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!)
        )
    }

    @Test
    fun `checkout() should confirm saved card payment methods`() = runTest {
        val stripeIntent = PAYMENT_INTENT
        val viewModel = spy(createViewModel(stripeIntent = stripeIntent))

        val expectedParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            clientSecret = stripeIntent.clientSecret!!,
            paymentMethodOptions = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            ),
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(expectedParams)

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        verify(viewModel).confirmStripeIntent(eq(expectedParams))
    }

    @Test
    fun `checkout() should confirm saved us_bank_account payment methods`() = runTest {
        val stripeIntent = PAYMENT_INTENT
        val viewModel = spy(createViewModel(stripeIntent = stripeIntent))

        val expectedParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = requireNotNull(PaymentMethodFixtures.US_BANK_ACCOUNT.id),
            clientSecret = stripeIntent.clientSecret!!,
            paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            mandateData = MandateDataParams(type = MandateDataParams.Type.Online.DEFAULT),
        )
        fakeIntentConfirmationInterceptor.enqueueConfirmStep(expectedParams)

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        verify(viewModel).confirmStripeIntent(eq(expectedParams))
    }

    @Test
    fun `checkout() for Setup Intent with saved payment method that requires mandate should include mandate`() = runTest {
        val stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        val viewModel = spy(
            createViewModel(
                stripeIntent = stripeIntent,
                args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
                    initializationMode = InitializationMode.SetupIntent(stripeIntent.clientSecret!!),
                ),
            )
        )

        val confirmParams = mock<ConfirmSetupIntentParams>()
        fakeIntentConfirmationInterceptor.enqueueConfirmStep(confirmParams)

        val paymentSelection = PaymentSelection.Saved(SEPA_DEBIT_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        verify(viewModel).confirmStripeIntent(confirmParams)
    }

    @Test
    fun `checkout() should confirm new payment methods`() = runTest {
        val stripeIntent = PAYMENT_INTENT
        val viewModel = spy(createViewModel(stripeIntent = stripeIntent))

        val paymentSelection = PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )

        val expectedParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            clientSecret = stripeIntent.clientSecret!!,
            paymentMethodOptions = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
            ),
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(expectedParams)

        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        verify(viewModel).confirmStripeIntent(eq(expectedParams))
    }

    @Test
    fun `checkout() with shipping should confirm new payment methods`() = runTest {
        val stripeIntent = PAYMENT_INTENT
        val viewModel = spy(
            createViewModel(
                stripeIntent = stripeIntent,
                args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                    config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config?.copy(
                        shippingDetails = AddressDetails(
                            address = PaymentSheet.Address(
                                country = "US"
                            ),
                            name = "Test Name"
                        )
                    )
                )
            )
        )

        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )

        val expectedParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            clientSecret = stripeIntent.clientSecret!!,
            paymentMethodOptions = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            shipping = ConfirmPaymentIntentParams.Shipping(
                address = Address(
                    country = "US"
                ),
                name = "Test Name"
            )
        )
        fakeIntentConfirmationInterceptor.enqueueConfirmStep(expectedParams)

        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        verify(viewModel).confirmStripeIntent(eq(expectedParams))
    }

    @Test
    fun `Enables Link when user is logged out of their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        assertThat(viewModel.linkHandler.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Does not enable Link when the Link state can't be determined`() = runTest {
        val viewModel = createViewModel(
            linkState = null,
        )

        assertThat(viewModel.linkHandler.isLinkEnabled.value).isFalse()
    }

    @Test
    fun `Google Pay checkout cancelled returns to Ready state`() = runTest {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        val googlePayButtonTurbine = viewModel.googlePayButtonState.testIn(this)
        val processingTurbine = viewModel.processing.testIn(this)

        assertThat(googlePayButtonTurbine.awaitItem())
            .isEqualTo(PaymentSheetViewState.StartProcessing)
        assertThat(processingTurbine.awaitItem()).isTrue()

        viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
        assertThat(viewModel.contentVisible.value).isTrue()

        assertThat(googlePayButtonTurbine.awaitItem())
            .isEqualTo(PaymentSheetViewState.Reset(null))
        assertThat(processingTurbine.awaitItem()).isFalse()

        googlePayButtonTurbine.cancel()
        processingTurbine.cancel()
    }

    @Test
    fun `On checkout clear the previous view state error`() = runTest {
        val viewModel = createViewModel()
        viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay

        val googlePayButtonTurbine = viewModel.googlePayButtonState.testIn(this)
        val buyButtonTurbine = viewModel.buyButtonState.testIn(this)

        assertThat(googlePayButtonTurbine.awaitItem())
            .isEqualTo(PaymentSheetViewState.Reset(null))

        viewModel.checkout()

        googlePayButtonTurbine.expectNoEvents()
        assertThat(buyButtonTurbine.awaitItem())
            .isEqualTo(PaymentSheetViewState.StartProcessing)

        googlePayButtonTurbine.cancel()
        buyButtonTurbine.cancel()
    }

    @Test
    fun `Google Pay checkout failed returns to Ready state and shows error`() = runTest {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        val googlePayButtonTurbine = viewModel.googlePayButtonState.testIn(this)
        val processingTurbine = viewModel.processing.testIn(this)

        assertThat(googlePayButtonTurbine.awaitItem())
            .isEqualTo(PaymentSheetViewState.StartProcessing)
        assertThat(processingTurbine.awaitItem()).isTrue()

        viewModel.onGooglePayResult(
            GooglePayPaymentMethodLauncher.Result.Failed(
                Exception("Test exception"),
                Status.RESULT_INTERNAL_ERROR.statusCode
            )
        )

        assertThat(viewModel.contentVisible.value).isTrue()
        assertThat(googlePayButtonTurbine.awaitItem())
            .isEqualTo(PaymentSheetViewState.Reset(UserErrorMessage("An internal error occurred.")))
        assertThat(processingTurbine.awaitItem()).isFalse()

        googlePayButtonTurbine.cancel()
        processingTurbine.cancel()
    }

    @Test
    fun `onPaymentResult() should update ViewState and save preferences`() =
        runTest {
            val viewModel = createViewModel()

            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            viewModel.updateSelection(selection)

            val resultTurbine = viewModel.paymentSheetResult.testIn(this)
            val viewStateTurbine = viewModel.viewState.testIn(this)

            viewModel.onPaymentResult(PaymentResult.Completed)

            assertThat(viewStateTurbine.awaitItem())
                .isEqualTo(PaymentSheetViewState.Reset(null))

            val finishedProcessingState = viewStateTurbine.awaitItem()
            assertThat(finishedProcessingState)
                .isInstanceOf(PaymentSheetViewState.FinishProcessing::class.java)

            (finishedProcessingState as PaymentSheetViewState.FinishProcessing).onComplete()

            assertThat(resultTurbine.awaitItem())
                .isEqualTo(PaymentSheetResult.Completed)

            verify(eventReporter)
                .onPaymentSuccess(
                    paymentSelection = selection,
                    currency = "usd",
                    deferredIntentConfirmationType = null,
                )
            assertThat(prefsRepository.paymentSelectionArgs)
                .containsExactly(selection)
            assertThat(
                prefsRepository.getSavedSelection(
                    isGooglePayAvailable = true,
                    isLinkAvailable = true
                )
            ).isEqualTo(
                SavedSelection.PaymentMethod(selection.paymentMethod.id.orEmpty())
            )

            resultTurbine.cancel()
            viewStateTurbine.cancel()
        }

    @Test
    fun `onPaymentResult() should update ViewState and not save new payment method`() =
        runTest {
            val viewModel = createViewModel(stripeIntent = PAYMENT_INTENT)

            val selection = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
            viewModel.updateSelection(selection)

            val resultTurbine = viewModel.paymentSheetResult.testIn(this)
            val viewStateTurbine = viewModel.viewState.testIn(this)

            viewModel.onPaymentResult(PaymentResult.Completed)

            assertThat(viewStateTurbine.awaitItem())
                .isEqualTo(PaymentSheetViewState.Reset(null))

            val finishedProcessingState = viewStateTurbine.awaitItem()
            assertThat(finishedProcessingState)
                .isInstanceOf(PaymentSheetViewState.FinishProcessing::class.java)

            (finishedProcessingState as PaymentSheetViewState.FinishProcessing).onComplete()

            assertThat(resultTurbine.awaitItem()).isEqualTo(PaymentSheetResult.Completed)

            verify(eventReporter)
                .onPaymentSuccess(
                    paymentSelection = selection,
                    currency = "usd",
                    deferredIntentConfirmationType = null,
                )

            assertThat(prefsRepository.paymentSelectionArgs).isEmpty()
            assertThat(
                prefsRepository.getSavedSelection(
                    isGooglePayAvailable = true,
                    isLinkAvailable = true
                )
            ).isEqualTo(SavedSelection.None)

            resultTurbine.cancel()
            viewStateTurbine.cancel()
        }

    @Test
    fun `onPaymentResult() with non-success outcome should report failure event`() = runTest {
        val viewModel = createViewModel()
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(selection)

        viewModel.stripeIntent.test {
            viewModel.onPaymentResult(PaymentResult.Failed(Throwable()))
            verify(eventReporter)
                .onPaymentFailure(
                    paymentSelection = selection,
                    currency = "usd",
                    isDecoupling = false,
                )

            val stripeIntent = awaitItem()
            assertThat(stripeIntent).isEqualTo(PAYMENT_INTENT)
        }
    }

    @Test
    fun `onPaymentResult() should update emit API errors`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.viewState.test {
                val errorMessage = "very helpful error message"
                viewModel.onPaymentResult(PaymentResult.Failed(Throwable(errorMessage)))

                assertThat(awaitItem())
                    .isEqualTo(
                        PaymentSheetViewState.Reset(null)
                    )
                assertThat(awaitItem())
                    .isEqualTo(
                        PaymentSheetViewState.Reset(
                            UserErrorMessage(errorMessage)
                        )
                    )
            }
        }

    @Test
    fun `fetchPaymentIntent() should update ViewState LiveData`() = runTest {
        val viewModel = createViewModel()
        viewModel.viewState.test {
            assertThat(awaitItem())
                .isEqualTo(
                    PaymentSheetViewState.Reset(null)
                )
        }
    }

    @Test
    fun `Loading payment sheet state should propagate errors`() = runTest {
        val viewModel = createViewModel(shouldFailLoad = true)
        viewModel.paymentSheetResult.test {
            assertThat(awaitItem())
                .isInstanceOf(PaymentSheetResult.Failed::class.java)
        }
    }

    @Test
    fun `Verify supported payment methods exclude afterpay if no shipping and no allow flag`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config?.copy(
                    shippingDetails = null,
                    allowsPaymentMethodsRequiringShippingAddress = false,
                )
            ),
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
                shipping = null,
            ),
        )

        assertThat(viewModel.supportedPaymentMethods).isEmpty()
    }

    @Test
    fun `Verify supported payment methods include afterpay if allow flag but no shipping`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config?.copy(
                    allowsPaymentMethodsRequiringShippingAddress = true,
                )
            ),
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
                shipping = null,
            ),
        )

        val expectedPaymentMethod = lpmRepository.fromCode("afterpay_clearpay")
        assertThat(viewModel.supportedPaymentMethods).containsExactly(expectedPaymentMethod)
    }

    @Test
    fun `Verify supported payment methods include afterpay if shipping but no allow flag`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config?.copy(
                    allowsPaymentMethodsRequiringShippingAddress = false,
                )
            ),
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
            ),
        )

        val expectedPaymentMethod = lpmRepository.fromCode("afterpay_clearpay")
        assertThat(viewModel.supportedPaymentMethods).containsExactly(expectedPaymentMethod)
    }

    @Test
    fun `Google Pay is not available if it's not ready`() = runTest {
        val viewModel = createViewModel(isGooglePayReady = false)
        viewModel.googlePayState.test {
            assertThat(awaitItem()).isEqualTo(GooglePayState.NotAvailable)
        }
    }

    @Test
    fun `Google Pay is available if it is ready`() = runTest {
        val viewModel = createViewModel(isGooglePayReady = true)
        viewModel.googlePayState.test {
            assertThat(awaitItem()).isEqualTo(GooglePayState.Available)
        }
    }

    @Test
    fun `googlePayLauncherConfig for SetupIntent with currencyCode should be valid`() {
        val viewModel = createViewModel(ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP)
        assertThat(viewModel.googlePayLauncherConfig)
            .isNotNull()
    }

    @Test
    fun `Should show amount is true for PaymentIntent`() {
        val viewModel = createViewModel()

        assertThat(viewModel.isProcessingPaymentIntent)
            .isTrue()
    }

    @Test
    fun `Should show amount is false for SetupIntent`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        )

        assertThat(viewModel.isProcessingPaymentIntent)
            .isFalse()
    }

    @Test
    fun `When configuration is empty, merchant name should reflect the app name`() {
        val viewModel = createViewModel(
            args = ARGS_WITHOUT_CUSTOMER
        )

        // In a real app, the app name will be used. In tests the package name is returned.
        assertThat(viewModel.merchantName)
            .isEqualTo("com.stripe.android.paymentsheet.test")
    }

    @Test
    fun `getSupportedPaymentMethods() filters payment methods with delayed settlement`() {
        val viewModel = createViewModel(
            stripeIntent = PAYMENT_INTENT.copy(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.Ideal.code,
                    PaymentMethod.Type.SepaDebit.code,
                    PaymentMethod.Type.Sofort.code,
                ),
            ),
        )

        assertThat(
            viewModel.supportedPaymentMethods
        ).containsExactly(
            lpmRepository.fromCode("card")!!,
            lpmRepository.fromCode("ideal")!!
        )
    }

    @Test
    fun `getSupportedPaymentMethods() does not filter payment methods when supportsDelayedSettlement = true`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    allowsDelayedPaymentMethods = true
                )
            ),
            stripeIntent = PAYMENT_INTENT.copy(
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code,
                    PaymentMethod.Type.Ideal.code,
                    PaymentMethod.Type.SepaDebit.code,
                    PaymentMethod.Type.Sofort.code,
                ),
            ),
        )

        assertThat(
            viewModel.supportedPaymentMethods
        ).containsExactly(
            lpmRepository.fromCode("card")!!,
            lpmRepository.fromCode("ideal")!!,
            lpmRepository.fromCode("sepa_debit")!!,
            lpmRepository.fromCode("sofort")!!
        )
    }

    @Test
    fun `Resets selection correctly after cancelling Google Pay`() = runTest {
        val viewModel = createViewModel()

        val initialSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
        )

        viewModel.selection.test {
            // New valid card
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(initialSelection)
            viewModel.transitionToAddPaymentScreen()
            assertThat(awaitItem()).isEqualTo(initialSelection)
            viewModel.checkoutWithGooglePay()
            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)

            // Still using the initial PaymentSelection
            expectNoEvents()
        }
    }

    @Test
    fun `updateSelection() posts mandate text when selected payment is us_bank_account`() {
        val viewModel = createViewModel()

        viewModel.updateSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
        )

        assertThat(viewModel.notesText.value)
            .isEqualTo(
                "By continuing, you agree to authorize payments pursuant to " +
                    "<a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
            )

        viewModel.updateSelection(
            PaymentSelection.New.GenericPaymentMethod(
                iconResource = 0,
                labelResource = "",
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            )
        )

        assertThat(viewModel.notesText.value)
            .isEqualTo(null)

        viewModel.updateSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(viewModel.notesText.value)
            .isEqualTo(null)
    }

    @Test
    fun `updatePrimaryButtonState updates the primary button state`() = runTest {
        val viewModel = createViewModel()

        viewModel.primaryButtonState.test {
            assertThat(awaitItem()).isNull()

            viewModel.updatePrimaryButtonState(PrimaryButton.State.Ready)

            assertThat(awaitItem()).isEqualTo(PrimaryButton.State.Ready)
        }
    }

    @Test
    fun `Content should be hidden when Google Pay is visible`() = runTest {
        val viewModel = createViewModel()
        viewModel.contentVisible.test {
            assertThat(awaitItem()).isTrue()
            viewModel.checkoutWithGooglePay()
            assertThat(awaitItem()).isFalse()
            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `setContentVisible updates content visible state`() = runTest {
        val viewModel = createViewModel()

        viewModel.contentVisible.test {
            // Initially true
            assertThat(awaitItem()).isTrue()

            viewModel.setContentVisible(false)

            assertThat(awaitItem()).isFalse()

            viewModel.setContentVisible(true)

            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `paymentMethods is not empty if customer has payment methods`() = runTest {
        val viewModel = createViewModel(
            customerPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        viewModel.paymentMethods.test {
            assertThat(awaitItem()).isNotEmpty()
        }
    }

    @Test
    fun `paymentMethods is empty if customer has no payment methods`() = runTest {
        val viewModel = createViewModel(customerPaymentMethods = emptyList())

        viewModel.paymentMethods.test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `paymentMethods is null if payment sheet state is not loaded`() = runTest {
        val viewModel = createViewModel(delay = Duration.INFINITE)

        viewModel.paymentMethods.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `handleBackPressed is consumed when processing is true`() = runTest {
        val viewModel = createViewModel(customerPaymentMethods = emptyList())
        viewModel.savedStateHandle[SAVE_PROCESSING] = true
        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(AddFirstPaymentMethod)
            viewModel.handleBackPressed()
        }
    }

    @Test
    fun `handleBackPressed delivers cancelled when pressing back on last screen`() = runTest {
        val viewModel = createViewModel(customerPaymentMethods = emptyList())
        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(AddFirstPaymentMethod)
            viewModel.paymentSheetResult.test {
                viewModel.handleBackPressed()
                assertThat(awaitItem()).isEqualTo(PaymentSheetResult.Canceled)
            }
        }
    }

    @Test
    fun `handleBackPressed goes from AddAnother to SelectSaved screen`() = runTest {
        val viewModel = createViewModel(
            customerPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        viewModel.transitionToAddPaymentScreen()
        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(AddAnotherPaymentMethod)
            viewModel.handleBackPressed()
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)
        }
    }

    @Test
    fun `current screen is AddFirstPaymentMethod if payment methods is empty`() = runTest {
        val viewModel = createViewModel(customerPaymentMethods = emptyList())

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(AddFirstPaymentMethod)
        }
    }

    @Test
    fun `current screen is SelectSavedPaymentMethods if payment methods is not empty`() = runTest {
        val viewModel = createViewModel(
            customerPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)
        }
    }

    @Test
    fun `Produces the correct form arguments when payment intent is off-session`() {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_OFF_SESSION,
        )

        val observedArgs = viewModel.createFormArguments(
            selectedItem = LpmRepository.HardcodedCard,
        )

        assertThat(observedArgs).isEqualTo(
            PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = LpmRepository.HardcodedCard.code,
                amount = Amount(
                    value = 1099,
                    currencyCode = "usd",
                ),
                showCheckbox = false,
                showCheckboxControlledFields = true,
                billingDetails = null,
            )
        )
    }

    @Test
    fun `Sends correct event when navigating to AddFirstPaymentMethod screen`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customerPaymentMethods = listOf(),
        )

        val receiver = viewModel.currentScreen.testIn(this)

        verify(eventReporter).onShowNewPaymentOptionForm(
            linkEnabled = eq(false),
            currency = eq("usd"),
            isDecoupling = eq(false),
        )

        receiver.cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `Sends correct event when navigating to AddFirstPaymentMethod screen with Link enabled`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.NeedsVerification,
            ),
            customerPaymentMethods = listOf(),
            customerRepository = FakeCustomerRepository(PAYMENT_METHODS)
        )

        val receiver = viewModel.currentScreen.testIn(this)

        verify(eventReporter).onShowNewPaymentOptionForm(
            linkEnabled = eq(true),
            currency = eq("usd"),
            isDecoupling = eq(false),
        )

        receiver.cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `Sends correct event when navigating to AddFirstPaymentMethod screen with active Link session`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedIn,
            ),
            customerPaymentMethods = listOf(),
            customerRepository = FakeCustomerRepository(PAYMENT_METHODS)
        )

        val receiver = viewModel.currentScreen.testIn(this)

        verify(eventReporter).onShowNewPaymentOptionForm(
            linkEnabled = eq(true),
            currency = eq("usd"),
            isDecoupling = eq(false),
        )

        receiver.cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `Sends correct event when navigating to AddAnotherPaymentMethod screen`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customerPaymentMethods = PaymentMethodFixtures.createCards(1),
        )

        val receiver = viewModel.currentScreen.testIn(this)

        verify(eventReporter).onShowExistingPaymentOptions(
            linkEnabled = eq(false),
            currency = eq("usd"),
            isDecoupling = eq(false),
        )

        viewModel.transitionToAddPaymentScreen()

        verify(eventReporter).onShowNewPaymentOptionForm(
            linkEnabled = eq(false),
            currency = eq("usd"),
            isDecoupling = eq(false),
        )

        receiver.cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `Sets editing to false when removing the last payment method while editing`() = runTest {
        val customerPaymentMethods = PaymentMethodFixtures.createCards(1)
        val viewModel = createViewModel(customerPaymentMethods = customerPaymentMethods)

        viewModel.editing.test {
            assertThat(awaitItem()).isFalse()

            viewModel.toggleEditing()
            assertThat(awaitItem()).isTrue()

            viewModel.removePaymentMethod(customerPaymentMethods.single())
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `Ignores payment selection while in edit mode`() = runTest {
        val viewModel = createViewModel().apply {
            updateSelection(PaymentSelection.Link)
        }

        viewModel.toggleEditing()
        viewModel.handlePaymentMethodSelected(PaymentSelection.GooglePay)

        assertThat(viewModel.selection.value).isEqualTo(PaymentSelection.Link)

        viewModel.toggleEditing()
        viewModel.handlePaymentMethodSelected(PaymentSelection.GooglePay)
        assertThat(viewModel.selection.value).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `updateSelection with new payment method updates the current selection`() = runTest {
        val viewModel = createViewModel()

        viewModel.selection.test {
            val newSelection = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
            )
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(newSelection)
            assertThat(awaitItem())
                .isEqualTo(newSelection)
            assertThat(viewModel.newPaymentSelection).isEqualTo(newSelection)
        }
    }

    @Test
    fun `updateSelection with saved payment method updates the current selection`() = runTest {
        val viewModel = createViewModel()

        viewModel.selection.test {
            val savedSelection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(savedSelection)
            assertThat(awaitItem()).isEqualTo(savedSelection)
            assertThat(viewModel.newPaymentSelection).isEqualTo(null)
        }
    }

    @Test
    fun `Resets the backstack if the last customer payment method is removed`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(1)
        val viewModel = createViewModel(customerPaymentMethods = paymentMethods)

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)
            viewModel.removePaymentMethod(paymentMethods.single())
            assertThat(awaitItem()).isEqualTo(AddFirstPaymentMethod)
        }
    }

    @Test
    fun `Shows Google Pay wallet button if Google Pay is available`() = runTest {
        val viewModel = createViewModel(isGooglePayReady = true)

        viewModel.walletsState.test {
            assertThat(awaitItem()?.googlePay).isNotNull()
        }
    }

    @Test
    fun `Hides Google Pay wallet button if Google Pay is not available`() = runTest {
        val viewModel = createViewModel(
            isGooglePayReady = false,
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        viewModel.walletsState.test {
            val state = awaitItem()
            assertThat(state).isNotNull()
            assertThat(state?.googlePay).isNull()
        }
    }

    @Test
    fun `Shows Link wallet button if Link is available`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedOut,
            )
        )

        viewModel.walletsState.test {
            assertThat(awaitItem()?.link).isNotNull()
            expectNoEvents()
        }
    }

    @Test
    fun `Hides Link wallet button if Link is not available`() = runTest {
        val intent = PAYMENT_INTENT.copy(paymentMethodTypes = listOf("card"))
        val viewModel = createViewModel(stripeIntent = intent, isGooglePayReady = true)

        viewModel.walletsState.test {
            val state = awaitItem()
            assertThat(state).isNotNull()
            assertThat(state?.link).isNull()
        }
    }

    @Test
    fun `Shows the correct divider text if intent only supports card`() = runTest {
        val intent = PAYMENT_INTENT.copy(paymentMethodTypes = listOf("card"))
        val viewModel = createViewModel(
            isGooglePayReady = true,
            stripeIntent = intent,
        )

        viewModel.walletsState.test {
            val textResource = awaitItem()?.dividerTextResource
            assertThat(textResource).isEqualTo(R.string.stripe_paymentsheet_or_pay_with_card)
        }
    }

    @Test
    fun `Shows the correct divider text if intent supports multiple payment method types`() = runTest {
        val intent = PAYMENT_INTENT.copy(paymentMethodTypes = listOf("card", "cashapp"))
        val viewModel = createViewModel(
            isGooglePayReady = true,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config?.copy(
                    allowsDelayedPaymentMethods = true,
                ),
            ),
            stripeIntent = intent,
        )

        viewModel.walletsState.test {
            val textResource = awaitItem()?.dividerTextResource
            assertThat(textResource).isEqualTo(R.string.stripe_paymentsheet_or_pay_using)
        }
    }

    @Test
    fun `Confirms intent if intent confirmation interceptor returns an unconfirmed intent`() = runTest {
        val viewModel = createViewModelForDeferredIntent().apply {
            registerFromActivity(DummyActivityResultCaller(), TestLifecycleOwner())
        }

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val resultClientSecret = "pi_123_secret_456"

        val expectedParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethod.id!!,
            clientSecret = resultClientSecret,
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(expectedParams)

        val savedSelection = PaymentSelection.Saved(paymentMethod)
        viewModel.updateSelection(savedSelection)
        viewModel.checkout()

        verify(paymentLauncher).confirm(eq(expectedParams))
    }

    @Test
    fun `Handles next action if intent confirmation interceptor returns an intent with an outstanding action`() = runTest {
        val viewModel = createViewModelForDeferredIntent().apply {
            registerFromActivity(DummyActivityResultCaller(), TestLifecycleOwner())
        }

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val resultClientSecret = "pi_123_secret_456"

        fakeIntentConfirmationInterceptor.enqueueNextActionStep(
            clientSecret = resultClientSecret,
        )

        val savedSelection = PaymentSelection.Saved(paymentMethod)
        viewModel.updateSelection(savedSelection)
        viewModel.checkout()

        verify(paymentLauncher).handleNextActionForPaymentIntent(eq(resultClientSecret))
    }

    @Test
    fun `Completes if intent confirmation interceptor returns a completed intent`() = runTest {
        val viewModel = createViewModelForDeferredIntent()

        viewModel.paymentSheetResult.test {
            val savedSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            viewModel.updateSelection(savedSelection)
            viewModel.checkout()

            fakeIntentConfirmationInterceptor.enqueueCompleteStep()

            val finishingState = viewModel.viewState.value as PaymentSheetViewState.FinishProcessing
            finishingState.onComplete()

            assertThat(awaitItem()).isEqualTo(PaymentSheetResult.Completed)
        }
    }

    @Test
    fun `Displays failure if intent confirmation interceptor returns a failure`() = runTest {
        val viewModel = createViewModelForDeferredIntent()

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val error = "oh boy this didn't work"

        viewModel.viewState.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetViewState.Reset())

            val savedSelection = PaymentSelection.Saved(paymentMethod)
            viewModel.updateSelection(savedSelection)
            viewModel.checkout()
            assertThat(awaitItem()).isEqualTo(PaymentSheetViewState.StartProcessing)

            fakeIntentConfirmationInterceptor.enqueueFailureStep(
                cause = Exception(error),
                message = error
            )
            assertThat(awaitItem()).isEqualTo(PaymentSheetViewState.Reset(UserErrorMessage(error)))
        }
    }

    @Test
    fun `Sends correct analytics event when using normal intent`() = runTest {
        createViewModel()

        verify(eventReporter).onInit(
            configuration = anyOrNull(),
            isDecoupling = eq(false),
        )
    }

    @Test
    fun `Sends correct analytics event when using deferred intent with client-side confirmation`() = runTest {
        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { _, _ ->
            throw AssertionError("Not expected to be called")
        }

        createViewModelForDeferredIntent()

        verify(eventReporter).onInit(
            configuration = anyOrNull(),
            isDecoupling = eq(true),
        )
    }

    @Test
    fun `Sends correct analytics event when using deferred intent with server-side confirmation`() = runTest {
        IntentConfirmationInterceptor.createIntentCallback =
            CreateIntentCallback { _, _ ->
                throw AssertionError("Not expected to be called")
            }

        createViewModelForDeferredIntent()

        verify(eventReporter).onInit(
            configuration = anyOrNull(),
            isDecoupling = eq(true),
        )
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

            val viewModel = createViewModelForDeferredIntent()

            val savedSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            viewModel.updateSelection(savedSelection)
            viewModel.checkout()

            val isForceSuccess = clientSecret == PaymentSheet.IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT
            fakeIntentConfirmationInterceptor.enqueueCompleteStep(isForceSuccess)

            verify(eventReporter).onPaymentSuccess(
                paymentSelection = eq(savedSelection),
                currency = anyOrNull(),
                deferredIntentConfirmationType = eq(deferredIntentConfirmationType),
            )
        }
    }

    @Test
    fun `Invalidates authenticator when lifecycle owner is destroyed`() {
        val lifecycleOwner = TestLifecycleOwner()
        val viewModel = createViewModel()

        viewModel.registerFromActivity(DummyActivityResultCaller(), lifecycleOwner)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(paymentLauncher.authenticatorRegistry).unregisterAuthenticator(any())
    }

    @Test
    fun `Sends no deferred_intent_confirmation_type for non-deferred intent confirmation`() = runTest {
        val viewModel = createViewModel()

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        viewModel.updateSelection(savedSelection)
        viewModel.checkout()

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethod.id!!,
            clientSecret = "pi_123_secret_456",
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(confirmParams)
        viewModel.onPaymentResult(PaymentResult.Completed)

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            currency = anyOrNull(),
            deferredIntentConfirmationType = isNull(),
        )
    }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for client-side confirmation of deferred intent`() = runTest {
        val viewModel = createViewModelForDeferredIntent()

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        viewModel.updateSelection(savedSelection)
        viewModel.checkout()

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethod.id!!,
            clientSecret = "pi_123_secret_456",
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = confirmParams,
            isDeferred = true,
        )
        viewModel.onPaymentResult(PaymentResult.Completed)

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            currency = anyOrNull(),
            deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Client),
        )
    }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for server-side confirmation of deferred intent`() = runTest {
        val viewModel = createViewModelForDeferredIntent()

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        viewModel.updateSelection(savedSelection)
        viewModel.checkout()

        fakeIntentConfirmationInterceptor.enqueueNextActionStep("pi_123_secret_456")
        viewModel.onPaymentResult(PaymentResult.Completed)

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            currency = anyOrNull(),
            deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Server),
        )
    }

    private fun createViewModel(
        args: PaymentSheetContractV2.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        stripeIntent: StripeIntent = PAYMENT_INTENT,
        customerPaymentMethods: List<PaymentMethod> = PAYMENT_METHODS,
        customerRepository: CustomerRepository = FakeCustomerRepository(customerPaymentMethods),
        shouldFailLoad: Boolean = false,
        linkState: LinkState? = null,
        isGooglePayReady: Boolean = false,
        delay: Duration = Duration.ZERO,
        lpmRepository: LpmRepository = this.lpmRepository,
    ): PaymentSheetViewModel {
        val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        return TestViewModelFactory.create(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
        ) { linkHandler, linkInteractor, savedStateHandle ->
            PaymentSheetViewModel(
                application,
                args,
                eventReporter,
                { paymentConfiguration },
                StripeIntentValidator(),
                FakePaymentSheetLoader(
                    stripeIntent = stripeIntent,
                    shouldFail = shouldFailLoad,
                    linkState = linkState,
                    customerPaymentMethods = customerPaymentMethods,
                    delay = delay,
                    isGooglePayAvailable = isGooglePayReady,
                ),
                customerRepository,
                prefsRepository,
                lpmRepository,
                paymentLauncherFactory = paymentLauncherFactory,
                mock(),
                Logger.noop(),
                testDispatcher,
                savedStateHandle = savedStateHandle,
                linkHandler = linkHandler,
                linkConfigurationCoordinator = linkInteractor,
                intentConfirmationInterceptor = fakeIntentConfirmationInterceptor,
                formViewModelSubComponentBuilderProvider = mock(),
            )
        }
    }

    private fun createViewModelForDeferredIntent(
        args: PaymentSheetContractV2.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        paymentIntent: PaymentIntent = PAYMENT_INTENT,
    ): PaymentSheetViewModel {
        val deferredIntent = paymentIntent.copy(id = null, clientSecret = null)

        val intentConfig = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = 12345, currency = "usd"),
        )

        return createViewModel(
            args = args.copy(initializationMode = InitializationMode.DeferredIntent(intentConfig)),
            stripeIntent = deferredIntent,
        )
    }

    private companion object {
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP =
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        private val ARGS_WITHOUT_CUSTOMER = PaymentSheetFixtures.ARGS_WITHOUT_CONFIG

        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
    }
}
