package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Build
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.android.gms.common.api.Status
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentSheet.InitializationMode
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException.PaymentIntentInTerminalState
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewAction
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.FakeEditPaymentMethodInteractorFactory
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.UserErrorMessage
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SessionTestRule
import com.stripe.android.ui.core.Amount
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.IntentConfirmationInterceptorTestRule
import com.stripe.android.utils.RelayingPaymentSheetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val sessionRule = SessionTestRule()

    @get:Rule
    val intentConfirmationInterceptorTestRule = IntentConfirmationInterceptorTestRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val prefsRepository = FakePrefsRepository()

    private val paymentLauncher = mock<StripePaymentLauncher>()

    private val paymentLauncherFactory = mock<StripePaymentLauncherAssistedFactory> {
        on { create(any(), any(), anyOrNull(), any(), any()) } doReturn paymentLauncher
    }
    private val googlePayLauncher = mock<GooglePayPaymentMethodLauncher>()
    private val googlePayLauncherFactory = mock<GooglePayPaymentMethodLauncherFactory> {
        on { create(any(), any(), any(), any(), any()) } doReturn googlePayLauncher
    }
    private val fakeIntentConfirmationInterceptor = FakeIntentConfirmationInterceptor()
    private val fakeEditPaymentMethodInteractorFactory = FakeEditPaymentMethodInteractorFactory(testDispatcher)

    private val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator()

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
            isDeferred = eq(false),
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
    fun `correct event is sent when dropdown is opened in EditPaymentMethod`() = runTest {
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            customerPaymentMethods = paymentMethods
        )

        viewModel.currentScreen.test {
            awaitItem()

            viewModel.modifyPaymentMethod(CARD_WITH_NETWORKS_PAYMENT_METHOD)

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf(PaymentSheetScreen.EditPaymentMethod::class.java)

            if (currentScreen is PaymentSheetScreen.EditPaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    EditPaymentMethodViewAction.OnBrandChoiceOptionsShown
                )

                verify(eventReporter).onShowPaymentOptionBrands(
                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                    selectedBrand = CardBrand.CartesBancaires
                )
            }
        }
    }

    @Test
    fun `correct event is sent when dropdown is dismissed in EditPaymentMethod`() = runTest {
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            customerPaymentMethods = paymentMethods
        )

        viewModel.currentScreen.test {
            awaitItem()

            viewModel.modifyPaymentMethod(CARD_WITH_NETWORKS_PAYMENT_METHOD)

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf(PaymentSheetScreen.EditPaymentMethod::class.java)

            if (currentScreen is PaymentSheetScreen.EditPaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    EditPaymentMethodViewAction.OnBrandChoiceOptionsDismissed
                )

                verify(eventReporter).onHidePaymentOptionBrands(
                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                    selectedBrand = null
                )
            }
        }
    }

    @Test
    fun `correct event is sent when dropdown is dismissed with change in EditPaymentMethod`() = runTest {
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            customerPaymentMethods = paymentMethods
        )

        viewModel.currentScreen.test {
            awaitItem()

            viewModel.modifyPaymentMethod(CARD_WITH_NETWORKS_PAYMENT_METHOD)

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf(PaymentSheetScreen.EditPaymentMethod::class.java)

            if (currentScreen is PaymentSheetScreen.EditPaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    EditPaymentMethodViewAction.OnBrandChoiceChanged(
                        EditPaymentMethodViewState.CardBrandChoice(CardBrand.Visa)
                    )
                )

                verify(eventReporter).onHidePaymentOptionBrands(
                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                    selectedBrand = CardBrand.Visa
                )
            }
        }
    }

    @Test
    fun `modifyPaymentMethod updates payment methods and sends event on successful update`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(5)

        val firstPaymentMethod = paymentMethods.first()

        val updatedPaymentMethod = firstPaymentMethod.copy(
            card = firstPaymentMethod.card?.copy(
                networks = firstPaymentMethod.card?.networks?.copy(
                    preferred = CardBrand.Visa.code
                )
            )
        )

        val customerRepository = spy(
            FakeCustomerRepository(
                onUpdatePaymentMethod = {
                    Result.success(updatedPaymentMethod)
                }
            )
        )
        val viewModel = createViewModel(
            customerPaymentMethods = paymentMethods,
            customerRepository = customerRepository
        )

        viewModel.currentScreen.test {
            awaitItem()

            viewModel.modifyPaymentMethod(firstPaymentMethod)

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf(PaymentSheetScreen.EditPaymentMethod::class.java)

            if (currentScreen is PaymentSheetScreen.EditPaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    EditPaymentMethodViewAction.OnBrandChoiceChanged(
                        EditPaymentMethodViewState.CardBrandChoice(CardBrand.Visa)
                    )
                )

                interactor.handleViewAction(EditPaymentMethodViewAction.OnUpdatePressed)
            }

            assertThat(awaitItem()).isInstanceOf(SelectSavedPaymentMethods::class.java)
        }

        verify(eventReporter).onUpdatePaymentMethodSucceeded(CardBrand.Visa)

        val idCaptor = argumentCaptor<String>()
        val paramsCaptor = argumentCaptor<PaymentMethodUpdateParams>()

        verify(customerRepository).updatePaymentMethod(
            any(),
            idCaptor.capture(),
            paramsCaptor.capture()
        )

        assertThat(idCaptor.firstValue).isEqualTo(firstPaymentMethod.id!!)

        assertThat(
            paramsCaptor.firstValue.toParamMap()
        ).isEqualTo(
            PaymentMethodUpdateParams.createCard(
                networks = PaymentMethodUpdateParams.Card.Networks(
                    preferred = CardBrand.Visa.code
                )
            ).toParamMap()
        )

        assertThat(viewModel.paymentMethods.value).isEqualTo(
            listOf(updatedPaymentMethod) + paymentMethods.takeLast(4)
        )
    }

    @Test
    fun `modifyPaymentMethod sends event on failed update`() = runTest {
        val paymentMethods = PaymentMethodFixtures.createCards(5)

        val firstPaymentMethod = paymentMethods.first()

        val customerRepository = spy(
            FakeCustomerRepository(
                onUpdatePaymentMethod = {
                    Result.failure(Exception("No network found!"))
                }
            )
        )
        val viewModel = createViewModel(
            customerPaymentMethods = paymentMethods,
            customerRepository = customerRepository
        )

        viewModel.currentScreen.test {
            awaitItem()

            viewModel.modifyPaymentMethod(firstPaymentMethod)

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf(PaymentSheetScreen.EditPaymentMethod::class.java)

            if (currentScreen is PaymentSheetScreen.EditPaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    EditPaymentMethodViewAction.OnBrandChoiceChanged(
                        EditPaymentMethodViewState.CardBrandChoice(CardBrand.Visa)
                    )
                )

                interactor.handleViewAction(EditPaymentMethodViewAction.OnUpdatePressed)
            }
        }

        verify(eventReporter).onUpdatePaymentMethodFailed(
            selectedBrand = eq(CardBrand.Visa),
            error = argThat {
                message == "No network found!"
            }
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
                    config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
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
    fun `Google Pay checkout cancelled returns to Idle state`() = runTest {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val processingTurbine = viewModel.processing.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Processing)
            assertThat(processingTurbine.awaitItem()).isTrue()

            viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(viewModel.contentVisible.value).isTrue()

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Idle(null))
            assertThat(processingTurbine.awaitItem()).isFalse()

            walletsProcessingStateTurbine.cancel()
            processingTurbine.cancel()
        }
    }

    @Test
    fun `On checkout clear the previous view state error`() = runTest {
        val viewModel = createViewModel()
        viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopWallet

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val buyButtonTurbine = viewModel.buyButtonState.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Idle(null))
            assertThat(buyButtonTurbine.awaitItem())
                .isEqualTo(null)

            viewModel.checkout()

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(null)
            assertThat(buyButtonTurbine.awaitItem())
                .isEqualTo(PaymentSheetViewState.StartProcessing)

            walletsProcessingStateTurbine.cancel()
            buyButtonTurbine.cancel()
        }
    }

    @Test
    fun `Google Pay checkout failed returns to Idle state and shows error`() = runTest {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val processingTurbine = viewModel.processing.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Processing)
            assertThat(processingTurbine.awaitItem()).isTrue()

            viewModel.onGooglePayResult(
                GooglePayPaymentMethodLauncher.Result.Failed(
                    Exception("Test exception"),
                    Status.RESULT_INTERNAL_ERROR.statusCode
                )
            )

            assertThat(viewModel.contentVisible.value).isTrue()
            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Idle(resolvableString("An internal error occurred.")))
            assertThat(processingTurbine.awaitItem()).isFalse()

            walletsProcessingStateTurbine.cancel()
            processingTurbine.cancel()
        }
    }

    @Test
    fun `On inline link payment, should process with primary button`() = runTest {
        val linkConfiguration = LinkTestUtils.createLinkConfiguration()

        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = linkConfiguration,
                loginState = LinkState.LoginState.LoggedOut
            )
        )

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val buyButtonStateTurbine = viewModel.buyButtonState.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(null)
            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(
                PaymentSheetViewState.Reset(null)
            )

            viewModel.updateSelection(
                PaymentSelection.New.Card(
                    paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    brand = CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                )
            )

            viewModel.updatePrimaryButtonForLinkSignup(
                InlineSignupViewState.create(
                    config = linkConfiguration
                ).copy(
                    userInput = UserInput.SignUp(
                        name = "John Doe",
                        email = "email@email.com",
                        phone = "+11234567890",
                        consentAction = SignUpConsentAction.Checkbox,
                        country = "CA",
                    )
                )
            )

            viewModel.checkout()

            walletsProcessingStateTurbine.expectNoEvents()

            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(PaymentSheetViewState.StartProcessing)

            fakeIntentConfirmationInterceptor.enqueueCompleteStep()

            assertThat(buyButtonStateTurbine.awaitItem()).isInstanceOf(
                PaymentSheetViewState.FinishProcessing::class.java
            )

            buyButtonStateTurbine.cancel()
            walletsProcessingStateTurbine.cancel()
        }
    }

    @Test
    fun `On inline link payment with save requested, should set with 'requireSaveOnConfirmation' set to 'true'`() =
        runTest {
            val intentConfirmationInterceptor = spy(fakeIntentConfirmationInterceptor)

            val viewModel = createLinkViewModel(intentConfirmationInterceptor)

            viewModel.linkHandler.payWithLinkInline(
                userInput = UserInput.SignIn("email@email.com"),
                paymentSelection = createCardPaymentSelection(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                ),
                shouldCompleteLinkInlineFlow = false
            )

            verify(intentConfirmationInterceptor).intercept(
                initializationMode = any(),
                paymentMethod = any(),
                shippingValues = isNull(),
                requiresSaveOnConfirmation = eq(true),
            )
        }

    @Test
    fun `On inline link payment with save not requested, should set with 'requireSaveOnConfirmation' set to 'false'`() =
        runTest {
            val intentConfirmationInterceptor = spy(fakeIntentConfirmationInterceptor)

            val viewModel = createLinkViewModel(intentConfirmationInterceptor)

            viewModel.linkHandler.payWithLinkInline(
                userInput = UserInput.SignIn("email@email.com"),
                paymentSelection = createCardPaymentSelection(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                ),
                shouldCompleteLinkInlineFlow = false
            )

            verify(intentConfirmationInterceptor).intercept(
                initializationMode = any(),
                paymentMethod = any(),
                shippingValues = isNull(),
                requiresSaveOnConfirmation = eq(false),
            )
        }

    @Test
    fun `On link payment through launcher, should process with wallets processing state`() = runTest {
        val linkConfiguration = LinkConfiguration(
            stripeIntent = mock {
                on { linkFundingSources } doReturn listOf(
                    PaymentMethod.Type.Card.code
                )
            },
            signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            customerInfo = LinkConfiguration.CustomerInfo(null, null, null, null),
            flags = mapOf(),
            merchantName = "Test merchant inc.",
            merchantCountryCode = "US",
            passthroughModeEnabled = false,
            shippingValues = mapOf(),
        )

        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = linkConfiguration,
                loginState = LinkState.LoginState.LoggedOut
            )
        )

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val buyButtonStateTurbine = viewModel.buyButtonState.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(null)
            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(
                PaymentSheetViewState.Reset(null)
            )

            viewModel.linkHandler.launchLink()

            assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(WalletsProcessingState.Processing)
            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(null)

            fakeIntentConfirmationInterceptor.enqueueCompleteStep()

            viewModel.linkHandler.onLinkActivityResult(
                LinkActivityResult.Completed(
                    paymentMethod = CARD_WITH_NETWORKS_PAYMENT_METHOD
                )
            )

            assertThat(walletsProcessingStateTurbine.awaitItem()).isInstanceOf(
                WalletsProcessingState.Completed::class.java
            )
            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(null)

            buyButtonStateTurbine.cancel()
            walletsProcessingStateTurbine.cancel()
        }
    }

    @Test
    fun `onPaymentResult() should update ViewState and save preferences`() =
        runTest {
            val viewModel = createViewModel()

            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            viewModel.updateSelection(selection)

            turbineScope {
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

            turbineScope {
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
        }

    @Test
    fun `onPaymentResult() with non-success outcome should report failure event`() = runTest {
        val viewModel = createViewModel()
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val error = APIException()

        viewModel.updateSelection(selection)

        viewModel.paymentMethodMetadata.test {
            viewModel.onPaymentResult(PaymentResult.Failed(error))
            verify(eventReporter)
                .onPaymentFailure(
                    paymentSelection = selection,
                    error = PaymentSheetConfirmationError.Stripe(error),
                )

            val stripeIntent = awaitItem()?.stripeIntent
            assertThat(stripeIntent).isEqualTo(PAYMENT_INTENT)
        }
    }

    @Test
    fun `onPaymentResult() should update emit generic error on IOExceptions`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.viewState.test {
                val errorMessage = "very helpful error message"
                viewModel.onPaymentResult(PaymentResult.Failed(IOException(errorMessage)))

                assertThat(awaitItem())
                    .isEqualTo(
                        PaymentSheetViewState.Reset(null)
                    )
                assertThat(awaitItem())
                    .isEqualTo(
                        PaymentSheetViewState.Reset(
                            UserErrorMessage("Something went wrong")
                        )
                    )
            }
        }

    @Test
    fun `onPaymentResult() should update emit Stripe API errors`() =
        runTest {
            val viewModel = createViewModel()

            viewModel.viewState.test {
                val errorMessage = "very helpful error message"
                val stripeError = StripeError(message = errorMessage)
                viewModel.onPaymentResult(PaymentResult.Failed(APIException(stripeError)))

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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    allowsPaymentMethodsRequiringShippingAddress = true,
                )
            ),
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
                shipping = null,
            ),
        )

        assertThat(viewModel.supportedPaymentMethods.map { it.code }).containsExactly("afterpay_clearpay")
    }

    @Test
    fun `Verify supported payment methods include afterpay if shipping but no allow flag`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    allowsPaymentMethodsRequiringShippingAddress = false,
                )
            ),
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
            ),
        )

        assertThat(viewModel.supportedPaymentMethods.map { it.code }).containsExactly("afterpay_clearpay")
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
    fun `'buttonType' from 'GooglePayConfiguration' should be parsed to proper 'GooglePayButtonType'`() = runTest {
        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Plain,
            GooglePayButtonType.Plain
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Pay,
            GooglePayButtonType.Pay
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Book,
            GooglePayButtonType.Book
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Buy,
            GooglePayButtonType.Buy
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Donate,
            GooglePayButtonType.Donate
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Checkout,
            GooglePayButtonType.Checkout
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Order,
            GooglePayButtonType.Order
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe,
            GooglePayButtonType.Subscribe
        )
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
            viewModel.supportedPaymentMethods.map { it.code }
        ).containsExactly("card", "ideal")
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
            viewModel.supportedPaymentMethods.map { it.code }
        ).containsExactly("card", "ideal", "sepa_debit", "sofort")
    }

    @Test
    fun `Resets selection correctly after cancelling Google Pay`() = runTest {
        val viewModel = createViewModel(initialPaymentSelection = null)

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

        assertThat(viewModel.mandateText.value?.text)
            .isEqualTo(
                "By continuing, you agree to authorize payments pursuant to " +
                    "<a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
            )
        assertThat(viewModel.mandateText.value?.showAbovePrimaryButton).isFalse()

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

        assertThat(viewModel.mandateText.value).isNull()

        viewModel.updateSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(viewModel.mandateText.value).isNull()
    }

    @Test
    fun `updateSelection() posts mandate text when selected payment is sepa`() {
        val viewModel = createViewModel()

        viewModel.updateSelection(
            PaymentSelection.Saved(SEPA_DEBIT_PAYMENT_METHOD)
        )

        assertThat(viewModel.mandateText.value?.text)
            .isEqualTo(
                "By providing your payment information and confirming this payment, you authorise (A) Merchant, Inc. " +
                    "and Stripe, our payment service provider, to send instructions to your bank to debit your " +
                    "account and (B) your bank to debit your account in accordance with those instructions. As part" +
                    " of your rights, you are entitled to a refund from your bank under the terms and conditions of" +
                    " your agreement with your bank. A refund must be claimed within 8 weeks starting from the date" +
                    " on which your account was debited. Your rights are explained in a statement that you can " +
                    "obtain from your bank. You agree to receive notifications for future debits up to 2 days before" +
                    " they occur."
            )
        assertThat(viewModel.mandateText.value?.showAbovePrimaryButton).isTrue()

        viewModel.updateSelection(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(viewModel.mandateText.value).isNull()
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
            selectedItem = LpmRepositoryTestHelpers.card,
        )

        assertThat(observedArgs).isEqualTo(
            PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = CardDefinition.type.code,
                amount = Amount(
                    value = 1099,
                    currencyCode = "usd",
                ),
                showCheckbox = false,
                billingDetails = PaymentSheet.BillingDetails(),
            )
        )
    }

    @Test
    fun `Sends correct event when navigating to AddFirstPaymentMethod screen`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customerPaymentMethods = listOf(),
        )

        turbineScope {
            val receiver = viewModel.currentScreen.testIn(this)

            verify(eventReporter).onShowNewPaymentOptionForm()

            receiver.cancelAndIgnoreRemainingEvents()
        }
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

        turbineScope {
            val receiver = viewModel.currentScreen.testIn(this)

            verify(eventReporter).onShowNewPaymentOptionForm()

            receiver.cancelAndIgnoreRemainingEvents()
        }
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

        turbineScope {
            val receiver = viewModel.currentScreen.testIn(this)

            verify(eventReporter).onShowNewPaymentOptionForm()

            receiver.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Sends correct event when navigating to EditPaymentMethod screen`() = runTest {
        val cards = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customerPaymentMethods = cards,
        )

        viewModel.modifyPaymentMethod(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        verify(eventReporter).onShowEditablePaymentOption()
    }

    @Test
    fun `Sends correct event when navigating out of EditPaymentMethod screen`() = runTest {
        val cards = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customerPaymentMethods = cards,
        )

        viewModel.modifyPaymentMethod(CARD_WITH_NETWORKS_PAYMENT_METHOD)
        viewModel.handleBackPressed()

        verify(eventReporter).onHideEditablePaymentOption()
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
        val viewModel = createViewModel(initialPaymentSelection = null)

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
        val viewModel = createViewModel(initialPaymentSelection = null)

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
    fun `Shows the correct divider text if PaymentIntent only supports card`() = runTest {
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
    fun `Shows the correct divider text if PaymentIntent supports multiple payment method types`() = runTest {
        val intent = PAYMENT_INTENT.copy(paymentMethodTypes = listOf("card", "cashapp"))
        val viewModel = createViewModel(
            isGooglePayReady = true,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
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
    fun `Shows the correct divider text if SetupIntent only supports card and`() = runTest {
        val intent = SETUP_INTENT.copy(paymentMethodTypes = listOf("card"))
        val viewModel = createViewModel(
            isGooglePayReady = true,
            stripeIntent = intent,
        )

        viewModel.walletsState.test {
            val textResource = awaitItem()?.dividerTextResource
            assertThat(textResource).isEqualTo(R.string.stripe_paymentsheet_or_use_a_card)
        }
    }

    @Test
    fun `Shows the correct divider text if SetupIntent supports multiple payment method types`() = runTest {
        val intent = SETUP_INTENT.copy(paymentMethodTypes = listOf("card", "cashapp"))
        val viewModel = createViewModel(
            isGooglePayReady = true,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    allowsDelayedPaymentMethods = true,
                ),
            ),
            stripeIntent = intent,
        )

        viewModel.walletsState.test {
            val textResource = awaitItem()?.dividerTextResource
            assertThat(textResource).isEqualTo(R.string.stripe_paymentsheet_or_use)
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
            isDeferred = eq(false),
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
            isDeferred = eq(true),
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
            isDeferred = eq(true),
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
                deferredIntentConfirmationType = eq(deferredIntentConfirmationType),
            )
        }
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
            deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Server),
        )
    }

    @Test
    fun `Sends dismiss event when the user cancels the flow with non-deferred intent`() = runTest {
        val viewModel = createViewModel()
        viewModel.onUserCancel()
        verify(eventReporter).onDismiss()
    }

    @Test
    fun `Sends dismiss event when the user cancels the flow with deferred intent`() = runTest {
        val viewModel = createViewModelForDeferredIntent()
        viewModel.onUserCancel()
        verify(eventReporter).onDismiss()
    }

    @Test
    fun `Sends confirm pressed event when fully confirming US bank account payment`() = runTest {
        val newPaymentSelection = PaymentSelection.New.USBankAccount(
            labelResource = "Test",
            iconResource = 0,
            paymentMethodCreateParams = mock(),
            customerRequestedSave = mock(),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = null,
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
            screenState = USBankAccountFormScreenState.SavedAccount(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = "Continue",
                mandateText = null,
            ),
        )

        val viewModel = createViewModel()

        viewModel.handleConfirmUSBankAccount(newPaymentSelection)

        verify(eventReporter).onPressConfirmButton(newPaymentSelection)
    }

    @Test
    fun `Sends no confirm pressed event when opening US bank account auth flow`() = runTest {
        val paymentIntent = PAYMENT_INTENT.copy(
            amount = 9999,
            currency = "CAD",
            paymentMethodTypes = listOf("card", "us_bank_account"),
        )

        val viewModel = createViewModel(stripeIntent = paymentIntent)

        // Mock the filled out US Bank Account form by updating the selection
        val usBankAccount = PaymentSelection.New.USBankAccount(
            labelResource = "Test",
            iconResource = 0,
            paymentMethodCreateParams = mock(),
            customerRequestedSave = mock(),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = null,
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
            screenState = USBankAccountFormScreenState.SavedAccount(
                financialConnectionsSessionId = "session_1234",
                intentId = "intent_1234",
                bankName = "Stripe Bank",
                last4 = "6789",
                primaryButtonText = "Continue",
                mandateText = null,
            ),
        )
        viewModel.updateSelection(usBankAccount)

        viewModel.checkout()

        verify(eventReporter, never()).onPressConfirmButton(any())
    }

    @Test
    fun `Launches Google Pay with custom label if provided for payment intent`() {
        val expectedLabel = "My custom label"
        val expectedAmount = 1099L

        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                    amount = 12345,
                    label = expectedLabel,
                )
            )
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
        )

        viewModel.setupGooglePay(
            lifecycleScope = mock(),
            activityResultLauncher = mock(),
        )

        viewModel.checkoutWithGooglePay()

        verify(googlePayLauncher).present(
            currencyCode = any(),
            amount = eq(expectedAmount),
            transactionId = anyOrNull(),
            label = eq(expectedLabel),
        )
    }

    @Test
    fun `Launches Google Pay with custom label and amount if provided for setup intent`() {
        val expectedLabel = "My custom label"
        val expectedAmount = 1234L

        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.copy(
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                    amount = expectedAmount,
                    label = expectedLabel,
                )
            )
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        viewModel.setupGooglePay(
            lifecycleScope = mock(),
            activityResultLauncher = mock(),
        )

        viewModel.checkoutWithGooglePay()

        verify(googlePayLauncher).present(
            currencyCode = any(),
            amount = eq(expectedAmount),
            transactionId = anyOrNull(),
            label = eq(expectedLabel),
        )
    }

    @Test
    fun `Launch confirmation form when Bacs debit is selected and filled & succeeds payment`() = runTest {
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
        val activityResultCaller = mock<ActivityResultCaller> {
            onGeneric {
                registerForActivityResult<BacsMandateConfirmationContract.Args, BacsMandateConfirmationResult>(
                    any(),
                    any()
                )
            } doReturn mock()
        }

        val viewModel = createViewModel(
            bacsMandateConfirmationLauncherFactory = launcherFactory
        ).apply {
            registerFromActivity(activityResultCaller, TestLifecycleOwner())
        }

        verify(activityResultCaller).registerForActivityResult(
            any<BacsMandateConfirmationContract>(),
            onResult.capture()
        )

        verify(launcherFactory).create(any())

        viewModel.updateSelection(
            createBacsPaymentSelection()
        )

        viewModel.checkout()

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

        viewModel.viewState.test {
            val viewState = awaitItem()

            assertThat(viewState).isInstanceOf(PaymentSheetViewState.FinishProcessing::class.java)
        }
    }

    @Test
    fun `Requires email and phone with Google Pay when collection mode is set to always`() {
        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                ),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                )
            )
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        val isEmailRequired = viewModel.googlePayLauncherConfig?.isEmailRequired
        val isBillingRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isRequired
        val isPhoneRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isPhoneNumberRequired

        assertThat(isEmailRequired).isTrue()
        assertThat(isBillingRequired).isTrue()
        assertThat(isPhoneRequired).isTrue()
    }

    @Test
    fun `Requires full billing details with Google Pay when collection mode is set to full`() {
        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                ),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                )
            )
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        val isBillingRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isRequired
        val format = viewModel.googlePayLauncherConfig?.billingAddressConfig?.format
        val isPhoneRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isPhoneNumberRequired

        assertThat(isBillingRequired).isTrue()
        assertThat(format).isEqualTo(GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full)
        assertThat(isPhoneRequired).isFalse()
    }

    @Test
    fun `Does not require email and phone with Google Pay when collection mode is not set to always`() {
        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                    email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                ),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                )
            )
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        val isEmailRequired = viewModel.googlePayLauncherConfig?.isEmailRequired
        val isBillingRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isRequired
        val isPhoneRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isPhoneNumberRequired

        assertThat(isEmailRequired).isFalse()
        assertThat(isBillingRequired).isFalse()
        assertThat(isPhoneRequired).isFalse()
    }

    @Test
    fun `Does not require billing details with Google Pay when collection mode is set to never`() {
        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.copy(
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                ),
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "CA",
                    currencyCode = "CAD",
                )
            )
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        val isEmailRequired = viewModel.googlePayLauncherConfig?.isEmailRequired
        val isBillingRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isRequired
        val isPhoneRequired = viewModel.googlePayLauncherConfig?.billingAddressConfig?.isPhoneNumberRequired

        assertThat(isEmailRequired).isFalse()
        assertThat(isBillingRequired).isFalse()
        assertThat(isPhoneRequired).isFalse()
    }

    @Test
    fun `On complete payment launcher result in PI mode & should reuse, should save payment selection`() = runTest {
        selectionSavedTest(
            initializationMode = InitializationMode.PaymentIntent(
                clientSecret = "pi_12345"
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
    }

    @Test
    fun `On complete payment launcher result in PI mode & should not reuse, should not save payment selection`() = runTest {
        selectionSavedTest(
            initializationMode = InitializationMode.PaymentIntent(
                clientSecret = "pi_12345"
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
            shouldSave = false
        )
    }

    @Test
    fun `On complete payment launcher result in SI mode, should save payment selection`() = runTest {
        selectionSavedTest(
            initializationMode = InitializationMode.SetupIntent(
                clientSecret = "si_123456"
            )
        )
    }

    @Test
    fun `On complete payment launcher result with PI config but no SFU, should not save payment selection`() = runTest {
        selectionSavedTest(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD"
                    )
                )
            ),
            shouldSave = false
        )
    }

    @Test
    fun `On complete payment launcher result with DI (PI+SFU), should save payment selection`() = runTest {
        selectionSavedTest(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD",
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession
                    )
                )
            )
        )
    }

    @Test
    fun `On complete payment launcher result with DI (SI), should save payment selection`() = runTest {
        selectionSavedTest(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                        currency = "USD"
                    )
                )
            )
        )
    }

    @Test
    fun `Returns payment success after process death if result is returned before state loads`() = runTest {
        testProcessDeathRestorationAfterPaymentSuccess(loadStateBeforePaymentResult = false)
    }

    @Test
    fun `Returns payment success after process death if state is loaded before result is returned`() = runTest {
        testProcessDeathRestorationAfterPaymentSuccess(loadStateBeforePaymentResult = true)
    }

    @Test
    fun `on initial navigation to AddPaymentMethod screen, should report form shown event`() = runTest {
        createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
            customerPaymentMethods = listOf()
        )

        verify(eventReporter).onPaymentMethodFormShown("card")
    }

    @Test
    fun `on cannot properly return from link or other lpms, should report event at maximum once`() = runTest {
        val viewModel = createViewModel()

        viewModel.cannotProperlyReturnFromLinkAndOtherLPMs()
        viewModel.cannotProperlyReturnFromLinkAndOtherLPMs()
        viewModel.cannotProperlyReturnFromLinkAndOtherLPMs()
        viewModel.cannotProperlyReturnFromLinkAndOtherLPMs()

        verify(eventReporter, times(1)).onCannotProperlyReturnFromLinkAndOtherLPMs()
    }

    @Test
    fun `on navigate to AddPaymentMethod screen, should report form shown event`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        viewModel.transitionToAddPaymentScreen()

        verify(eventReporter).onPaymentMethodFormShown("card")
    }

    @Test
    fun `on payment form changed, should report form shown event`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
            customerPaymentMethods = listOf()
        )

        viewModel.reportPaymentMethodTypeSelected("us_bank_account")

        verify(eventReporter).onPaymentMethodFormShown("us_bank_account")
    }

    @Test
    fun `on form changed to same value, should report form shown event only once`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
            customerPaymentMethods = listOf()
        )

        viewModel.reportPaymentMethodTypeSelected("us_bank_account")
        viewModel.reportPaymentMethodTypeSelected("us_bank_account")
        viewModel.reportPaymentMethodTypeSelected("us_bank_account")

        verify(eventReporter, times(1)).onPaymentMethodFormShown("us_bank_account")
    }

    @Test
    fun `on leaving form and returning, should report form shown event on each navigation event`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        viewModel.transitionToAddPaymentScreen()
        viewModel.handleBackPressed()
        viewModel.transitionToAddPaymentScreen()

        verify(eventReporter, times(2)).onPaymentMethodFormShown("card")
    }

    @Test
    fun `on field interaction, should report event`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        viewModel.reportFieldInteraction("card")

        verify(eventReporter).onPaymentMethodFormInteraction("card")
    }

    @Test
    fun `on multiple field interactions with same payment form, should report event only once`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        viewModel.reportFieldInteraction("card")
        viewModel.reportFieldInteraction("card")
        viewModel.reportFieldInteraction("card")

        verify(eventReporter, times(1)).onPaymentMethodFormInteraction("card")
    }

    @Test
    fun `on card number completed event, should report event`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        viewModel.reportCardNumberCompleted()

        verify(eventReporter).onCardNumberCompleted()
    }

    @Test
    fun `on 'removePaymentMethod' with no CustomerConfiguration available, should not attempt detach`() = runTest {
        val customerRepository = spy(FakeCustomerRepository())
        val viewModel = createViewModel(
            customerPaymentMethods = PAYMENT_METHODS,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    customer = null
                )
            ),
        )

        viewModel.removePaymentMethod(PAYMENT_METHODS.first())

        verify(customerRepository, never()).detachPaymentMethod(any(), any())
    }

    @Test
    fun `on 'modifyPaymentMethod' with no CustomerConfiguration available, should not attempt update`() = runTest {
        val customerRepository = spy(FakeCustomerRepository())

        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            customerPaymentMethods = paymentMethods,
            customerRepository = customerRepository,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    customer = null
                )
            ),
        )

        viewModel.currentScreen.test {
            awaitItem()

            viewModel.modifyPaymentMethod(CARD_WITH_NETWORKS_PAYMENT_METHOD)

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf(PaymentSheetScreen.EditPaymentMethod::class.java)

            if (currentScreen is PaymentSheetScreen.EditPaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    EditPaymentMethodViewAction.OnBrandChoiceChanged(
                        EditPaymentMethodViewState.CardBrandChoice(CardBrand.Visa)
                    )
                )

                verify(customerRepository, never()).updatePaymentMethod(any(), any(), any())
            }
        }
    }

    private suspend fun testProcessDeathRestorationAfterPaymentSuccess(loadStateBeforePaymentResult: Boolean) {
        val stripeIntent = PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
        val savedStateHandle = SavedStateHandle(initialState = mapOf("AwaitingPaymentResult" to true))
        val paymentSheetLoader = RelayingPaymentSheetLoader()

        val viewModel = createViewModel(
            stripeIntent = stripeIntent,
            savedStateHandle = savedStateHandle,
            paymentSheetLoader = paymentSheetLoader,
        )

        val resultListener = viewModel.capturePaymentResultListener()

        fun loadState() {
            paymentSheetLoader.enqueueSuccess(
                stripeIntent = stripeIntent,
                validationError = PaymentIntentInTerminalState(StripeIntent.Status.Succeeded),
            )
        }

        fun emitPaymentResult() {
            resultListener.onActivityResult(InternalPaymentResult.Completed(stripeIntent))
        }

        viewModel.paymentSheetResult.test {
            expectNoEvents()

            if (loadStateBeforePaymentResult) {
                loadState()
            } else {
                emitPaymentResult()
            }

            expectNoEvents()

            if (loadStateBeforePaymentResult) {
                emitPaymentResult()
            } else {
                loadState()
            }

            assertThat(awaitItem()).isEqualTo(PaymentSheetResult.Completed)
        }
    }

    private suspend fun selectionSavedTest(
        initializationMode: InitializationMode = ARGS_CUSTOMER_WITH_GOOGLEPAY.initializationMode,
        customerRequestedSave: PaymentSelection.CustomerRequestedSave =
            PaymentSelection.CustomerRequestedSave.NoRequest,
        shouldSave: Boolean = true
    ) {
        val intent = PAYMENT_INTENT_WITH_PAYMENT_METHOD!!

        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                initializationMode = initializationMode
            ),
            stripeIntent = PAYMENT_INTENT
        )

        val paymentResultListener = viewModel.capturePaymentResultListener()

        val selection = PaymentSelection.New.Card(
            brand = CardBrand.Visa,
            customerRequestedSave = customerRequestedSave,
            paymentMethodCreateParams = PaymentMethodCreateParams.create(
                card = PaymentMethodCreateParams.Card()
            )
        )

        viewModel.updateSelection(selection)

        paymentResultListener.onActivityResult(InternalPaymentResult.Completed(intent))

        val savedSelection = PaymentSelection.Saved(
            paymentMethod = intent.paymentMethod!!
        )

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

    private fun createViewModel(
        args: PaymentSheetContractV2.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        stripeIntent: StripeIntent = PAYMENT_INTENT,
        customerPaymentMethods: List<PaymentMethod> = PAYMENT_METHODS,
        intentConfirmationInterceptor: IntentConfirmationInterceptor = fakeIntentConfirmationInterceptor,
        linkConfigurationCoordinator: LinkConfigurationCoordinator = this.linkConfigurationCoordinator,
        customerRepository: CustomerRepository = FakeCustomerRepository(customerPaymentMethods),
        shouldFailLoad: Boolean = false,
        linkState: LinkState? = null,
        isGooglePayReady: Boolean = false,
        delay: Duration = Duration.ZERO,
        initialPaymentSelection: PaymentSelection? =
            customerPaymentMethods.firstOrNull()?.let { PaymentSelection.Saved(it) },
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory = mock(),
        validationError: PaymentSheetLoadingException? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        paymentSheetLoader: PaymentSheetLoader = FakePaymentSheetLoader(
            stripeIntent = stripeIntent,
            shouldFail = shouldFailLoad,
            linkState = linkState,
            customerPaymentMethods = customerPaymentMethods,
            delay = delay,
            isGooglePayAvailable = isGooglePayReady,
            paymentSelection = initialPaymentSelection,
            validationError = validationError,
        ),
    ): PaymentSheetViewModel {
        val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        return TestViewModelFactory.create(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            savedStateHandle = savedStateHandle,
        ) { linkHandler, linkInteractor, thisSavedStateHandle ->
            PaymentSheetViewModel(
                application = application,
                args = args,
                eventReporter = eventReporter,
                lazyPaymentConfig = { paymentConfiguration },
                paymentSheetLoader = paymentSheetLoader,
                customerRepository = customerRepository,
                prefsRepository = prefsRepository,
                paymentLauncherFactory = paymentLauncherFactory,
                googlePayPaymentMethodLauncherFactory = googlePayLauncherFactory,
                bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
                logger = Logger.noop(),
                workContext = testDispatcher,
                savedStateHandle = thisSavedStateHandle,
                linkHandler = linkHandler,
                linkConfigurationCoordinator = linkInteractor,
                intentConfirmationInterceptor = intentConfirmationInterceptor,
                editInteractorFactory = fakeEditPaymentMethodInteractorFactory
            )
        }
    }

    private fun createLinkViewModel(
        intentConfirmationInterceptor: IntentConfirmationInterceptor = fakeIntentConfirmationInterceptor
    ): PaymentSheetViewModel {
        val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
            attachNewCardToAccountResult = Result.success(LinkTestUtils.LINK_SAVED_PAYMENT_DETAILS),
            accountStatus = AccountStatus.Verified,
        )

        return createViewModel(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                loginState = LinkState.LoginState.LoggedOut
            )
        )
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

    private suspend fun testButtonTypeParsedToProperGooglePayButtonType(
        buttonType: PaymentSheet.GooglePayConfiguration.ButtonType,
        googlePayButtonType: GooglePayButtonType
    ) {
        val viewModel = createViewModel(
            isGooglePayReady = true,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.copy(
                    googlePay = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.googlePayConfig?.copy(
                        buttonType = buttonType
                    )
                )
            )
        )

        viewModel.walletsState.test {
            assertThat(awaitItem()?.googlePay?.buttonType).isEqualTo(googlePayButtonType)
        }
    }

    private fun createCardPaymentSelection(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave
    ): PaymentSelection {
        return PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            brand = CardBrand.Visa,
            customerRequestedSave = customerRequestedSave,
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

    private fun PaymentSheetViewModel.capturePaymentResultListener(): ActivityResultCallback<InternalPaymentResult> {
        val mockActivityResultCaller = mock<ActivityResultCaller> {
            on {
                registerForActivityResult<PaymentLauncherContract.Args, InternalPaymentResult>(any(), any())
            } doReturn mock()
        }

        registerFromActivity(mockActivityResultCaller, TestLifecycleOwner())

        val paymentResultListenerCaptor = argumentCaptor<ActivityResultCallback<InternalPaymentResult>>()

        verify(mockActivityResultCaller).registerForActivityResult(
            any<PaymentLauncherContract>(),
            paymentResultListenerCaptor.capture(),
        )

        return paymentResultListenerCaptor.firstValue
    }

    private companion object {
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP =
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY

        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val PAYMENT_INTENT_WITH_PAYMENT_METHOD = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD
        val SETUP_INTENT = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD

        private const val BACS_ACCOUNT_NUMBER = "00012345"
        private const val BACS_SORT_CODE = "108800"
        private const val BACS_NAME = "John Doe"
        private const val BACS_EMAIL = "johndoe@email.com"
    }
}
