package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Build
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.viewModelScope
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
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_SELECTION
import com.stripe.android.model.PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createTestConfirmationHandlerFactory
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.PaymentSheetFixtures.ARGS_DEFERRED_INTENT
import com.stripe.android.paymentsheet.PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.FakeCvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.RecordingCvcRecollectionLauncherFactory
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.PaymentSheetViewState.UserErrorMessage
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationContract
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationResult
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateData
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.Args
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcCompletionState
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException.PaymentIntentInTerminalState
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.ui.CardBrandChoice
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SessionTestRule
import com.stripe.android.ui.core.Amount
import com.stripe.android.utils.BankFormScreenStateFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.IntentConfirmationInterceptorTestRule
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.RecordingLinkPaymentLauncher
import com.stripe.android.utils.RelayingPaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import com.stripe.android.R as PaymentsCoreR

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
        on { create(any(), any(), any(), any(), any(), any()) } doReturn googlePayLauncher
    }
    private val fakeIntentConfirmationInterceptor = FakeIntentConfirmationInterceptor()
    private val cvcRecollectionHandler = FakeCvcRecollectionHandler()

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
    fun `correct event is sent when dropdown is opened in EditPaymentMethod`() = runTest {
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods)
        )

        viewModel.navigationHandler.currentScreen.test {
            awaitItem()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
            )

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf<PaymentSheetScreen.UpdatePaymentMethod>()

            if (currentScreen is PaymentSheetScreen.UpdatePaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    UpdatePaymentMethodInteractor.ViewAction.BrandChoiceOptionsShown
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
            customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods)
        )

        viewModel.navigationHandler.currentScreen.test {
            awaitItem()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
            )

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf<PaymentSheetScreen.UpdatePaymentMethod>()

            if (currentScreen is PaymentSheetScreen.UpdatePaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    UpdatePaymentMethodInteractor.ViewAction.BrandChoiceOptionsDismissed
                )

                verify(eventReporter).onHidePaymentOptionBrands(
                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                    selectedBrand = CardBrand.CartesBancaires,
                )
            }
        }
    }

    @Test
    fun `correct event is sent when dropdown is dismissed with change in EditPaymentMethod`() = runTest {
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods)
        )

        viewModel.navigationHandler.currentScreen.test {
            awaitItem()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
            )

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf<PaymentSheetScreen.UpdatePaymentMethod>()

            if (currentScreen is PaymentSheetScreen.UpdatePaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                        CardBrandChoice(CardBrand.Visa)
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
    @Suppress("LongMethod")
    fun `modifyPaymentMethod should use loaded customer info when modifying payment methods`() = runTest {
        val paymentMethods = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val customerRepository = spy(
            FakeCustomerRepository(
                onUpdatePaymentMethod = {
                    Result.success(paymentMethods.first())
                }
            )
        )
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123"
                    )
                )
            ),
            customer = CustomerState(
                id = "cus_2",
                ephemeralKeySecret = "ek_123",
                customerSessionClientSecret = null,
                paymentMethods = paymentMethods,
                permissions = CustomerState.Permissions(
                    canRemovePaymentMethods = true,
                    canRemoveLastPaymentMethod = true,
                    canRemoveDuplicates = false,
                ),
                defaultPaymentMethodId = null
            ),
            customerRepository = customerRepository
        )

        viewModel.navigationHandler.currentScreen.test {
            awaitItem()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                paymentMethods.first().toDisplayableSavedPaymentMethod()
            )

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf<PaymentSheetScreen.UpdatePaymentMethod>()

            if (currentScreen is PaymentSheetScreen.UpdatePaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                        CardBrandChoice(CardBrand.Visa)
                    )
                )

                interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)
            }

            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
        }

        val customerInfoCaptor = argumentCaptor<CustomerRepository.CustomerInfo>()

        verify(customerRepository).updatePaymentMethod(
            customerInfoCaptor.capture(),
            any(),
            any()
        )

        assertThat(customerInfoCaptor.firstValue).isEqualTo(
            CustomerRepository.CustomerInfo(
                id = "cus_2",
                ephemeralKeySecret = "ek_123",
                customerSessionClientSecret = null,
            )
        )
    }

    @Test
    @Suppress("LongMethod")
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
            customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods),
            customerRepository = customerRepository
        )

        viewModel.navigationHandler.currentScreen.test {
            awaitItem()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                firstPaymentMethod.toDisplayableSavedPaymentMethod()
            )

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf<PaymentSheetScreen.UpdatePaymentMethod>()

            if (currentScreen is PaymentSheetScreen.UpdatePaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                        CardBrandChoice(CardBrand.Visa)
                    )
                )

                interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)
            }

            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
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

        assertThat(viewModel.customerStateHolder.paymentMethods.value).isEqualTo(
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
            customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods),
            customerRepository = customerRepository
        )

        viewModel.navigationHandler.currentScreen.test {
            awaitItem()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                firstPaymentMethod.toDisplayableSavedPaymentMethod()
            )

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf<PaymentSheetScreen.UpdatePaymentMethod>()

            if (currentScreen is PaymentSheetScreen.UpdatePaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                        CardBrandChoice(CardBrand.Visa)
                    )
                )

                interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)
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

        val paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        verify(paymentLauncher).confirm(eq(expectedParams))
    }

    @Test
    fun `checkout() with null payment selection, should report event and show failure`() = runTest {
        val errorReporter = FakeErrorReporter()
        val viewModel = createViewModel(
            errorReporter = errorReporter,
            initialPaymentSelection = null,
        )

        viewModel.error.test {
            assertThat(awaitItem()).isNull()

            viewModel.checkout()

            assertThat(errorReporter.getLoggedErrors()).contains(
                "unexpected_error.paymentsheet.no_payment_selection"
            )

            assertThat(awaitItem()).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        }
    }

    @Test
    fun `checkout() with invalid payment selection, should report event and show failure`() = runTest {
        val errorReporter = FakeErrorReporter()
        val viewModel = createViewModel(
            errorReporter = errorReporter,
            initialPaymentSelection = null,
        )

        viewModel.error.test {
            assertThat(awaitItem()).isNull()

            viewModel.updateSelection(PaymentSelection.Link)
            viewModel.checkout()

            assertThat(errorReporter.getLoggedErrors()).contains(
                "unexpected_error.paymentsheet.invalid_payment_selection"
            )

            assertThat(awaitItem()).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        }
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

        verify(paymentLauncher).confirm(eq(expectedParams))
    }

    @Test
    fun `checkout() for Setup Intent with saved payment method that requires mandate should include mandate`() =
        runTest {
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

            verify(paymentLauncher).confirm(eq(confirmParams))
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

        verify(paymentLauncher).confirm(eq(expectedParams))
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

        verify(paymentLauncher).confirm(eq(expectedParams))
    }

    @Test
    fun `Enables Link when user is logged out of their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
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
        val googlePayListener = viewModel.captureGooglePayListener()

        viewModel.checkoutWithGooglePay()

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val processingTurbine = viewModel.processing.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Processing)
            assertThat(processingTurbine.awaitItem()).isTrue()

            googlePayListener.onActivityResult(GooglePayPaymentMethodLauncher.Result.Canceled)
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
        val googlePayListener = viewModel.captureGooglePayListener()

        viewModel.checkoutWithGooglePay()

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val processingTurbine = viewModel.processing.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Processing)
            assertThat(processingTurbine.awaitItem()).isTrue()

            googlePayListener.onActivityResult(
                GooglePayPaymentMethodLauncher.Result.Failed(
                    Exception("Test exception"),
                    Status.RESULT_INTERNAL_ERROR.statusCode
                )
            )

            assertThat(viewModel.contentVisible.value).isTrue()
            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Idle(PaymentsCoreR.string.stripe_internal_error.resolvableString))
            assertThat(processingTurbine.awaitItem()).isFalse()

            walletsProcessingStateTurbine.cancel()
            processingTurbine.cancel()
        }
    }

    @Test
    fun `On inline link payment, should process with primary button`() = runTest {
        val linkConfiguration = LinkTestUtils.createLinkConfiguration()
        val signupMode = LinkSignupMode.InsteadOfSaveForFutureUse

        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = linkConfiguration,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = signupMode,
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

            val linkInlineHandler = LinkInlineHandler.create(viewModel, viewModel.viewModelScope)
            linkInlineHandler.onStateUpdated(
                InlineSignupViewState.create(
                    signupMode = signupMode,
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

            assertThat(buyButtonStateTurbine.awaitItem()).isInstanceOf<
                PaymentSheetViewState.FinishProcessing
                >()

            buyButtonStateTurbine.cancel()
            walletsProcessingStateTurbine.cancel()
        }
    }

    @Test
    fun `On inline link payment with save requested, should set 'paymentMethodOptionsParams' SFU to off_session`() =
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
                paymentMethodOptionsParams = eq(
                    PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    )
                ),
                shippingValues = isNull(),
            )
        }

    @Test
    fun `On inline link payment with save not requested, should set 'paymentMethodOptionsParams' SFU to blank`() =
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
                paymentMethodOptionsParams = eq(
                    PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                    )
                ),
                shippingValues = isNull(),
            )
        }

    @Test
    fun `On link payment through launcher, should process with wallets processing state`() = runTest {
        RecordingLinkPaymentLauncher.test {
            val linkConfiguration = LinkConfiguration(
                stripeIntent = mock {
                    on { linkFundingSources } doReturn listOf(
                        PaymentMethod.Type.Card.code
                    )
                },
                customerInfo = LinkConfiguration.CustomerInfo(null, null, null, null),
                flags = mapOf(),
                merchantName = "Test merchant inc.",
                merchantCountryCode = "US",
                passthroughModeEnabled = false,
                cardBrandChoice = null,
                shippingDetails = null,
                useAttestationEndpointsForLink = false,
            )

            val viewModel = createViewModel(
                linkState = LinkState(
                    configuration = linkConfiguration,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
                ),
                linkPaymentLauncher = launcher,
            )

            turbineScope {
                val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
                val buyButtonStateTurbine = viewModel.buyButtonState.testIn(this)

                assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(null)
                assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(
                    PaymentSheetViewState.Reset(null)
                )

                viewModel.checkoutWithLink()

                assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(WalletsProcessingState.Processing)
                assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(null)

                fakeIntentConfirmationInterceptor.enqueueCompleteStep()

                val registerCall = registerCalls.awaitItem()

                assertThat(presentCalls.awaitItem()).isNotNull()

                registerCall.callback(
                    LinkActivityResult.PaymentMethodObtained(
                        paymentMethod = CARD_WITH_NETWORKS_PAYMENT_METHOD
                    )
                )

                assertThat(walletsProcessingStateTurbine.awaitItem()).isInstanceOf<WalletsProcessingState.Completed>()

                buyButtonStateTurbine.cancel()
                walletsProcessingStateTurbine.cancel()
            }
        }
    }

    @Test
    fun `onPaymentResult() should update ViewState and save preferences`() =
        runTest {
            val viewModel = createViewModel()

            val selection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
            viewModel.updateSelection(selection)

            turbineScope {
                val resultTurbine = viewModel.paymentSheetResult.testIn(this)
                val viewStateTurbine = viewModel.viewState.testIn(this)

                viewModel.onPaymentResult(PaymentResult.Completed)

                assertThat(viewStateTurbine.awaitItem())
                    .isEqualTo(PaymentSheetViewState.Reset(null))

                val finishedProcessingState = viewStateTurbine.awaitItem()
                assertThat(finishedProcessingState)
                    .isInstanceOf<PaymentSheetViewState.FinishProcessing>()

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
                    .isInstanceOf<PaymentSheetViewState.FinishProcessing>()

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
        val selection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
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
    fun `On fail due to invalid deferred intent usage, should report with expected integration error`() = runTest {
        val interceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueFailureStep(
                cause = InvalidDeferredIntentUsageException(),
                message = "An error occurred!",
            )
        }

        val eventReporter = FakeEventReporter()
        val viewModel = createViewModel(
            intentConfirmationInterceptor = interceptor,
            eventReporter = eventReporter,
        )

        viewModel.updateSelection(CARD_PAYMENT_SELECTION)
        viewModel.checkout()

        val error = eventReporter.paymentFailureCalls.awaitItem().error

        assertThat(error.analyticsValue).isEqualTo("invalidDeferredIntentUsage")
        assertThat(error.cause).isInstanceOf(InvalidDeferredIntentUsageException::class.java)
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
                            UserErrorMessage(R.string.stripe_something_went_wrong.resolvableString)
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
                            UserErrorMessage(errorMessage.resolvableString)
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
                .isInstanceOf<PaymentSheetResult.Failed>()
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

        assertThat(viewModel.supportedPaymentMethodTypes).isEmpty()
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

        assertThat(viewModel.supportedPaymentMethodTypes).containsExactly("afterpay_clearpay")
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

        assertThat(viewModel.supportedPaymentMethodTypes).containsExactly("afterpay_clearpay")
    }

    @Test
    fun `Google Pay is not available if it's not ready`() = runTest {
        val viewModel = createViewModel(isGooglePayReady = false)
        viewModel.paymentMethodMetadata.test {
            assertThat(awaitItem()?.isGooglePayReady).isFalse()
        }
    }

    @Test
    fun `Google Pay is available if it is ready`() = runTest {
        val viewModel = createViewModel(isGooglePayReady = true)
        viewModel.paymentMethodMetadata.test {
            assertThat(awaitItem()?.isGooglePayReady).isTrue()
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
            viewModel.supportedPaymentMethodTypes
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
            viewModel.supportedPaymentMethodTypes
        ).containsExactly("card", "ideal", "sepa_debit", "sofort")
    }

    @Test
    fun `Resets selection correctly after cancelling Google Pay`() = runTest {
        val viewModel = createViewModel(initialPaymentSelection = null)
        val googlePayListener = viewModel.captureGooglePayListener()

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
            googlePayListener.onActivityResult(GooglePayPaymentMethodLauncher.Result.Canceled)

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

        assertThat(viewModel.mandateHandler.mandateText.value?.text?.resolve(application))
            .isEqualTo(
                "By continuing, you agree to authorize payments pursuant to " +
                    "<a href=\"https://stripe.com/ach-payments/authorization\">these terms</a>."
            )
        assertThat(viewModel.mandateHandler.mandateText.value?.showAbovePrimaryButton).isFalse()

        viewModel.updateSelection(
            PaymentSelection.New.GenericPaymentMethod(
                iconResource = 0,
                label = "".resolvableString,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            )
        )

        assertThat(viewModel.mandateHandler.mandateText.value).isNull()

        viewModel.updateSelection(
            PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        )

        assertThat(viewModel.mandateHandler.mandateText.value).isNull()
    }

    @Test
    fun `updateSelection() posts mandate text when selected payment is sepa`() {
        val viewModel = createViewModel()

        viewModel.updateSelection(
            PaymentSelection.Saved(SEPA_DEBIT_PAYMENT_METHOD)
        )

        assertThat(viewModel.mandateHandler.mandateText.value?.text?.resolve(application))
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
        assertThat(viewModel.mandateHandler.mandateText.value?.showAbovePrimaryButton).isTrue()

        viewModel.updateSelection(
            PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        )

        assertThat(viewModel.mandateHandler.mandateText.value).isNull()
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
        val googlePayListener = viewModel.captureGooglePayListener()
        viewModel.contentVisible.test {
            assertThat(awaitItem()).isTrue()
            viewModel.checkoutWithGooglePay()
            assertThat(awaitItem()).isFalse()
            googlePayListener.onActivityResult(GooglePayPaymentMethodLauncher.Result.Canceled)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `launched with correct screen when in horizontal mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal
                )
            ),
        )
        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
        }
    }

    @Test
    fun `launched with correct screen when in vertical mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical
                )
            ),
        )
        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalMode>()
        }
    }

    @Test
    fun `launched with correct screen when in automatic mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic
                )
            ),
        )
        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<PaymentSheetScreen.VerticalMode>()
        }
    }

    @Test
    fun `handleBackPressed is consumed when processing is true`() = runTest {
        val viewModel = createViewModel(customer = EMPTY_CUSTOMER_STATE)
        viewModel.savedStateHandle[SAVE_PROCESSING] = true
        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<AddFirstPaymentMethod>()
            viewModel.handleBackPressed()
        }
    }

    @Test
    fun `handleBackPressed delivers cancelled when pressing back on last screen`() = runTest {
        val viewModel = createViewModel(customer = EMPTY_CUSTOMER_STATE)
        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<AddFirstPaymentMethod>()
            viewModel.paymentSheetResult.test {
                viewModel.handleBackPressed()
                assertThat(awaitItem()).isEqualTo(PaymentSheetResult.Canceled)
            }
        }
    }

    @Test
    fun `handleBackPressed goes from AddAnother to SelectSaved screen`() = runTest {
        val viewModel = createViewModel(
            customer = EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = listOf(CARD_PAYMENT_METHOD)
            )
        )
        viewModel.transitionToAddPaymentScreen()
        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<AddAnotherPaymentMethod>()
            viewModel.handleBackPressed()
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
        }
    }

    @Test
    fun `current screen is AddFirstPaymentMethod if payment methods is empty`() = runTest {
        val viewModel = createViewModel(customer = EMPTY_CUSTOMER_STATE)

        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<AddFirstPaymentMethod>()
        }
    }

    @Test
    fun `current screen is SelectSavedPaymentMethods if payment methods is not empty`() = runTest {
        val viewModel = createViewModel(
            customer = EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = listOf(CARD_PAYMENT_METHOD)
            )
        )

        viewModel.navigationHandler.currentScreen.test {
            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
        }
    }

    @Test
    fun `Produces the correct form arguments when payment intent is off-session`() {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_OFF_SESSION,
        )

        val observedArgs = DefaultFormHelper.create(
            viewModel = viewModel,
            linkInlineHandler = LinkInlineHandler.create(viewModel, viewModel.viewModelScope),
            paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
        ).createFormArguments(
            paymentMethodCode = LpmRepositoryTestHelpers.card.code,
        )

        assertThat(observedArgs).isEqualTo(
            PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = CardDefinition.type.code,
                amount = Amount(
                    value = 1099,
                    currencyCode = "usd",
                ),
                hasIntentToSetup = true,
                billingDetails = PaymentSheet.BillingDetails(),
            )
        )
    }

    @Test
    fun `Sends correct event when navigating to AddFirstPaymentMethod screen`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customer = null,
        )

        turbineScope {
            val receiver = viewModel.navigationHandler.currentScreen.testIn(this)

            verify(eventReporter).onShowNewPaymentOptions()

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
                signupMode = null,
            ),
            customer = null,
            customerRepository = FakeCustomerRepository(PAYMENT_METHODS)
        )

        turbineScope {
            val receiver = viewModel.navigationHandler.currentScreen.testIn(this)

            verify(eventReporter).onShowNewPaymentOptions()

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
                signupMode = null,
            ),
            customer = null,
            customerRepository = FakeCustomerRepository(PAYMENT_METHODS)
        )

        turbineScope {
            val receiver = viewModel.navigationHandler.currentScreen.testIn(this)

            verify(eventReporter).onShowNewPaymentOptions()

            receiver.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Sends correct event when navigating to EditPaymentMethod screen`() = runTest {
        val cards = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = cards),
        )

        viewModel.savedPaymentMethodMutator.updatePaymentMethod(
            CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
        )

        verify(eventReporter).onShowEditablePaymentOption()
    }

    @Test
    fun `Sends correct event when navigating out of EditPaymentMethod screen`() = runTest {
        val cards = listOf(CARD_WITH_NETWORKS_PAYMENT_METHOD)

        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = cards),
        )

        viewModel.savedPaymentMethodMutator.updatePaymentMethod(
            CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
        )
        viewModel.handleBackPressed()

        verify(eventReporter).onHideEditablePaymentOption()
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
            assertThat(viewModel.newPaymentSelection).isEqualTo(
                NewOrExternalPaymentSelection.New(
                    newSelection
                )
            )
        }
    }

    @Test
    fun `updateSelection with external payment method updates the current selection`() = runTest {
        val viewModel = createViewModel(initialPaymentSelection = null)

        viewModel.selection.test {
            val newSelection = PaymentSelection.ExternalPaymentMethod(
                type = "external_fawry",
                billingDetails = null,
                label = "Fawry".resolvableString,
                iconResource = 0,
                lightThemeIconUrl = "some_url",
                darkThemeIconUrl = null,
            )
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(newSelection)
            assertThat(awaitItem()).isEqualTo(newSelection)
            assertThat(viewModel.newPaymentSelection).isEqualTo(
                NewOrExternalPaymentSelection.External(
                    newSelection
                )
            )
        }
    }

    @Test
    fun `updateSelection with saved payment method updates the current selection`() = runTest {
        val viewModel = createViewModel(initialPaymentSelection = null)

        viewModel.selection.test {
            val savedSelection = PaymentSelection.Saved(
                CARD_PAYMENT_METHOD
            )
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(savedSelection)
            assertThat(awaitItem()).isEqualTo(savedSelection)
            assertThat(viewModel.newPaymentSelection).isEqualTo(null)
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
                signupMode = null,
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
                signupMode = null,
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
            registerFromActivity(DummyActivityResultCaller.noOp(), TestLifecycleOwner())
        }

        val paymentMethod = CARD_PAYMENT_METHOD
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
    fun `Handles next action if intent confirmation interceptor returns an intent with an outstanding action`() =
        runTest {
            val viewModel = createViewModelForDeferredIntent().apply {
                registerFromActivity(DummyActivityResultCaller.noOp(), TestLifecycleOwner())
            }

            val paymentMethod = CARD_PAYMENT_METHOD
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
            val savedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
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

        val paymentMethod = CARD_PAYMENT_METHOD
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
            assertThat(awaitItem()).isEqualTo(PaymentSheetViewState.Reset(UserErrorMessage(error.resolvableString)))
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

            val savedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
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

        val paymentMethod = CARD_PAYMENT_METHOD
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
        val callback = viewModel.capturePaymentResultListener()

        val paymentMethod = CARD_PAYMENT_METHOD
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
        callback.onActivityResult(InternalPaymentResult.Completed(PAYMENT_INTENT))

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.Client),
        )
    }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for server-side confirmation of deferred intent`() = runTest {
        val viewModel = createViewModelForDeferredIntent()
        val callback = viewModel.capturePaymentResultListener()

        val paymentMethod = CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        viewModel.updateSelection(savedSelection)
        viewModel.checkout()

        fakeIntentConfirmationInterceptor.enqueueNextActionStep("pi_123_secret_456")
        callback.onActivityResult(InternalPaymentResult.Completed(PAYMENT_INTENT))

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
    fun `Sends no confirm pressed event when opening US bank account auth flow`() = runTest {
        val paymentIntent = PAYMENT_INTENT.copy(
            amount = 9999,
            currency = "CAD",
            paymentMethodTypes = listOf("card", "us_bank_account"),
        )

        val viewModel = createViewModel(stripeIntent = paymentIntent)

        // Mock the filled out US Bank Account form by updating the selection
        val usBankAccount = PaymentSelection.New.USBankAccount(
            label = "Test",
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
            instantDebits = null,
            screenState = BankFormScreenStateFactory.createWithSession("session_1234"),
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
            bacsMandateConfirmationLauncherFactory = launcherFactory,
            shouldRegister = false,
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

            assertThat(viewState).isInstanceOf<PaymentSheetViewState.FinishProcessing>()
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
    fun `Can complete payment after switching to another LPM from card selection with inline Link signup state`() =
        runTest {
            Dispatchers.setMain(testDispatcher)

            val interceptor = spy(FakeIntentConfirmationInterceptor())

            val signupMode = LinkSignupMode.InsteadOfSaveForFutureUse
            val viewModel = spy(
                createViewModel(
                    customer = EMPTY_CUSTOMER_STATE,
                    intentConfirmationInterceptor = interceptor,
                    linkState = LinkState(LINK_CONFIG, LinkState.LoginState.LoggedOut, signupMode)
                )
            )

            viewModel.primaryButtonUiState.test {
                assertThat(awaitItem()?.enabled).isFalse()

                viewModel.updateSelection(
                    PaymentSelection.New.Card(
                        paymentMethodCreateParams = PaymentMethodCreateParams.createCard(
                            CardParams(
                                number = "4242424242424242",
                                expMonth = 4,
                                expYear = 2025,
                                cvc = "501"
                            )
                        ),
                        brand = CardBrand.Visa,
                        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                    )
                )

                assertThat(awaitItem()?.enabled).isTrue()

                val linkInlineHandler = LinkInlineHandler.create(viewModel, viewModel.viewModelScope)
                linkInlineHandler.onStateUpdated(
                    InlineSignupViewState.create(
                        signupMode = signupMode,
                        config = LINK_CONFIG,
                    ).copy(
                        isExpanded = true,
                        userInput = UserInput.SignUp(
                            name = "John Doe",
                            email = "johndoe@email.com",
                            phone = "+15555555555",
                            consentAction = SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone,
                            country = "US",
                        )
                    )
                )

                assertThat(awaitItem()?.enabled).isTrue()

                viewModel.updateSelection(
                    PaymentSelection.New.GenericPaymentMethod(
                        iconResource = R.drawable.stripe_ic_paymentsheet_card_visa,
                        label = "Bancontact".resolvableString,
                        paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.BANCONTACT,
                        customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                        lightThemeIconUrl = null,
                        darkThemeIconUrl = null,
                    )
                )

                val buyButton = awaitItem()

                assertThat(buyButton?.enabled).isTrue()

                buyButton?.onClick?.invoke()

                assertThat(awaitItem()?.enabled).isFalse()
            }

            verify(interceptor).intercept(
                initializationMode = any(),
                paymentMethodCreateParams = any(),
                paymentMethodOptionsParams = isNull(),
                shippingValues = isNull(),
                customerRequestedSave = eq(false),
            )
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
    fun `On complete payment launcher result in PI mode & should not reuse, should not save payment selection`() =
        runTest {
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
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
            customer = EMPTY_CUSTOMER_STATE,
        )

        verify(eventReporter).onPaymentMethodFormShown("card")
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
    fun `on 'modifyPaymentMethod' with no customer available, should not attempt update`() = runTest {
        val customerRepository = spy(FakeCustomerRepository())

        val viewModel = createViewModel(
            customer = null,
            customerRepository = customerRepository,
        )

        viewModel.navigationHandler.currentScreen.test {
            awaitItem()

            viewModel.savedPaymentMethodMutator.updatePaymentMethod(
                CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
            )

            val currentScreen = awaitItem()

            assertThat(currentScreen).isInstanceOf<PaymentSheetScreen.UpdatePaymentMethod>()

            if (currentScreen is PaymentSheetScreen.UpdatePaymentMethod) {
                val interactor = currentScreen.interactor

                interactor.handleViewAction(
                    UpdatePaymentMethodInteractor.ViewAction.BrandChoiceChanged(
                        CardBrandChoice(CardBrand.Visa)
                    )
                )

                verify(customerRepository, never()).updatePaymentMethod(any(), any(), any())
            }
        }
    }

    @Test
    fun `isCvcRecollectionEnabled returns paymentMethodOptionsJsonString value or false if null`() = runTest {
        val viewModel = createViewModel()

        cvcRecollectionHandler.cvcRecollectionEnabled = false
        assertThat(viewModel.isCvcRecollectionEnabled()).isFalse()

        cvcRecollectionHandler.cvcRecollectionEnabled = true
        assertThat(viewModel.isCvcRecollectionEnabled()).isTrue()
    }

    @Test
    fun `CurrentScreen is SelectSavedPaymentMethods with correct CVC Recollection State`() = runTest {
        val stripeIntent = PaymentIntentFactory.create(
            paymentMethodOptionsJsonString = getPaymentMethodOptionJsonStringWithCvcRecollectionValue(true)
        )
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        val viewModel = createViewModel(
            customer = EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = listOf(CARD_PAYMENT_METHOD)
            ),
            stripeIntent = stripeIntent
        )
        viewModel.navigationHandler.currentScreen.test {
            val screen = awaitItem()
            assertThat(screen).isInstanceOf<SelectSavedPaymentMethods>()
            assertThat(
                (screen as SelectSavedPaymentMethods).cvcRecollectionState
            ).isInstanceOf<SelectSavedPaymentMethods.CvcRecollectionState.Required>()
        }
    }

    @Test
    fun `getCvcRecollectionState returns correct state for complete flow`() = runTest {
        var stripeIntent = PaymentIntentFactory.create(
            paymentMethodOptionsJsonString = getPaymentMethodOptionJsonStringWithCvcRecollectionValue(true)
        )
        var viewModel = createViewModel(
            stripeIntent = stripeIntent
        )

        cvcRecollectionHandler.cvcRecollectionEnabled = true

        assertThat(viewModel.getCvcRecollectionState())
            .isInstanceOf<SelectSavedPaymentMethods.CvcRecollectionState.Required>()

        stripeIntent = PaymentIntentFactory.create(
            paymentMethodOptionsJsonString = getPaymentMethodOptionJsonStringWithCvcRecollectionValue(false)
        )
        viewModel = createViewModel(
            stripeIntent = stripeIntent
        )

        cvcRecollectionHandler.cvcRecollectionEnabled = false

        assertThat(viewModel.getCvcRecollectionState())
            .isInstanceOf<SelectSavedPaymentMethods.CvcRecollectionState.NotRequired>()
    }

    @Test
    fun `On confirm with existing payment method, calls interceptor with expected parameters`() = runTest {
        val initializationMode = InitializationMode.PaymentIntent(clientSecret = "pi_123")
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val intentConfirmationInterceptor = FakeIntentConfirmationInterceptor()
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                initializationMode = initializationMode,
            ),
            stripeIntent = intent,
            intentConfirmationInterceptor = intentConfirmationInterceptor
        )

        val paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        val call = intentConfirmationInterceptor.calls.awaitItem()

        assertThat(call).isEqualTo(
            FakeIntentConfirmationInterceptor.InterceptCall.WithExistingPaymentMethod(
                initializationMode = initializationMode,
                paymentMethod = CARD_PAYMENT_METHOD,
                paymentMethodOptionsParams = null,
                shippingValues = null,
            )
        )
    }

    @Test
    fun `getCvcRecollectionState returns correct state for deferred flow`() = runTest {
        cvcRecollectionHandler.cvcRecollectionEnabled = false
        val viewModel = createViewModel(args = ARGS_DEFERRED_INTENT)

        assertThat(viewModel.getCvcRecollectionState())
            .isInstanceOf<SelectSavedPaymentMethods.CvcRecollectionState.NotRequired>()

        cvcRecollectionHandler.cvcRecollectionEnabled = true

        assertThat(viewModel.getCvcRecollectionState())
            .isInstanceOf<SelectSavedPaymentMethods.CvcRecollectionState.Required>()

        cvcRecollectionHandler.cvcRecollectionEnabled = false

        assertThat(viewModel.getCvcRecollectionState())
            .isInstanceOf<SelectSavedPaymentMethods.CvcRecollectionState.NotRequired>()
    }

    @Test
    fun `requiresCvcRecollection should return correct value`() {
        var viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical
                )
            )
        )

        val savedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)

        cvcRecollectionHandler.requiresCVCRecollection = true
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isTrue()
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()

        viewModel.checkout()
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()

        cvcRecollectionHandler.requiresCVCRecollection = false
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()

        viewModel = createViewModel()

        cvcRecollectionHandler.requiresCVCRecollection = true
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isTrue()

        cvcRecollectionHandler.requiresCVCRecollection = false
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()
    }

    @Test
    fun `requiresCvcRecollection should return correct value in automatic mode`() {
        var viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic
                )
            )
        )

        val savedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)

        cvcRecollectionHandler.requiresCVCRecollection = true
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isTrue()
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()

        viewModel.checkout()
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()

        cvcRecollectionHandler.requiresCVCRecollection = false
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()

        viewModel = createViewModel()

        cvcRecollectionHandler.requiresCVCRecollection = true
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isTrue()

        cvcRecollectionHandler.requiresCVCRecollection = false
        assertThat(viewModel.shouldAttachCvc(savedSelection)).isFalse()
        assertThat(viewModel.shouldLaunchCvcRecollectionScreen(savedSelection)).isFalse()
    }

    @Test
    fun `CvcRecollection screen should be displayed on checkout when required in vertical mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical
                )
            ),
        )

        cvcRecollectionHandler.requiresCVCRecollection = true
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        viewModel.checkout()

        viewModel.navigationHandler.currentScreen.test {
            val screen = awaitItem()
            assertThat(screen).isInstanceOf<PaymentSheetScreen.CvcRecollection>()
        }
    }

    @Test
    fun `CvcRecollection screen should be displayed on checkout when required in automatic mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic
                )
            ),
        )

        cvcRecollectionHandler.requiresCVCRecollection = true
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        viewModel.checkout()

        viewModel.navigationHandler.currentScreen.test {
            val screen = awaitItem()
            assertThat(screen).isInstanceOf<PaymentSheetScreen.CvcRecollection>()
        }
    }

    @Test
    fun `CvcRecollection state update should update payment selection`() = runTest {
        val cvcRecollectionInteractor = FakeCvcRecollectionInteractor()
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical
                )
            ),
            cvcRecollectionInteractor = cvcRecollectionInteractor
        )

        cvcRecollectionHandler.requiresCVCRecollection = true
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        viewModel.checkout()

        viewModel.selection.test {
            awaitItem()

            cvcRecollectionInteractor.updateCompletionState(CvcCompletionState.Completed("444"))
            assertThat(selectionCvc(awaitItem())).isEqualTo("444")

            cvcRecollectionInteractor.updateCompletionState(CvcCompletionState.Incomplete)
            assertThat(selectionCvc(awaitItem())).isEqualTo("")
        }
    }

    @Test
    fun `CvcRecollection screen should not be displayed on checkout when required but in horizontal mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY
        )

        cvcRecollectionHandler.requiresCVCRecollection = true
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        viewModel.checkout()

        viewModel.navigationHandler.currentScreen.test {
            val screen = awaitItem()
            assertThat(screen).isInstanceOf<SelectSavedPaymentMethods>()
        }
    }

    @Test
    fun `CvcRecollection screen should not be displayed on checkout when not required in vertical mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical
                )
            ),
        )

        cvcRecollectionHandler.requiresCVCRecollection = false
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        viewModel.checkout()

        viewModel.navigationHandler.currentScreen.test {
            val screen = awaitItem()
            assertThat(screen).isInstanceOf<PaymentSheetScreen.VerticalMode>()
        }
    }

    @Test
    fun `CvcRecollection screen should not be displayed on checkout when not required in automatic mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic
                )
            ),
        )

        cvcRecollectionHandler.requiresCVCRecollection = false
        cvcRecollectionHandler.cvcRecollectionEnabled = true
        viewModel.checkout()

        viewModel.navigationHandler.currentScreen.test {
            val screen = awaitItem()
            assertThat(screen).isInstanceOf<PaymentSheetScreen.VerticalMode>()
        }
    }

    private suspend fun testProcessDeathRestorationAfterPaymentSuccess(loadStateBeforePaymentResult: Boolean) {
        val stripeIntent = PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
        val option = PaymentMethodConfirmationOption.Saved(
            paymentMethod = CARD_PAYMENT_METHOD,
            optionsParams = null,
        )
        val savedStateHandle = SavedStateHandle(
            initialState = mapOf(
                "AwaitingConfirmationResult" to DefaultConfirmationHandler.AwaitingConfirmationResultData(
                    key = "Intent",
                    confirmationOption = option,
                    receivesResultInProcess = false,
                ),
                "IntentConfirmationParameters" to ConfirmationMediator.Parameters(
                    confirmationOption = option,
                    deferredIntentConfirmationType = null,
                    confirmationParameters = ConfirmationDefinition.Parameters(
                        intent = PAYMENT_INTENT,
                        initializationMode = ARGS_CUSTOMER_WITH_GOOGLEPAY.initializationMode,
                        shippingDetails = null,
                        appearance = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.appearance,
                    )
                )
            )
        )
        val paymentSheetLoader = RelayingPaymentElementLoader()

        val viewModel = createViewModel(
            stripeIntent = stripeIntent,
            savedStateHandle = savedStateHandle,
            paymentElementLoader = paymentSheetLoader,
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

        val createParams = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card()
        )
        val selection = PaymentSelection.New.Card(
            brand = CardBrand.Visa,
            customerRequestedSave = customerRequestedSave,
            paymentMethodCreateParams = createParams
        )

        fakeIntentConfirmationInterceptor.enqueueConfirmStep(
            confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = createParams,
                clientSecret = "pi_1234"
            )
        )

        viewModel.updateSelection(selection)
        viewModel.checkout()

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
        customer: CustomerState? = EMPTY_CUSTOMER_STATE.copy(paymentMethods = PAYMENT_METHODS),
        intentConfirmationInterceptor: IntentConfirmationInterceptor = fakeIntentConfirmationInterceptor,
        linkPaymentLauncher: LinkPaymentLauncher = RecordingLinkPaymentLauncher.noOp(),
        linkConfigurationCoordinator: LinkConfigurationCoordinator = this.linkConfigurationCoordinator,
        customerRepository: CustomerRepository = FakeCustomerRepository(customer?.paymentMethods ?: emptyList()),
        shouldFailLoad: Boolean = false,
        linkState: LinkState? = null,
        isGooglePayReady: Boolean = false,
        delay: Duration = Duration.ZERO,
        initialPaymentSelection: PaymentSelection? =
            customer?.paymentMethods?.firstOrNull()?.let { PaymentSelection.Saved(it) },
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory = mock(),
        paymentLauncherFactory: StripePaymentLauncherAssistedFactory = this.paymentLauncherFactory,
        validationError: PaymentSheetLoadingException? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        paymentElementLoader: PaymentElementLoader = FakePaymentElementLoader(
            stripeIntent = stripeIntent,
            shouldFail = shouldFailLoad,
            linkState = linkState,
            customer = customer,
            delay = delay,
            isGooglePayAvailable = isGooglePayReady,
            paymentSelection = initialPaymentSelection,
            validationError = validationError,
        ),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        eventReporter: EventReporter = this.eventReporter,
        cvcRecollectionHandler: CvcRecollectionHandler = this.cvcRecollectionHandler,
        cvcRecollectionInteractor: FakeCvcRecollectionInteractor = FakeCvcRecollectionInteractor(),
        shouldRegister: Boolean = true,
    ): PaymentSheetViewModel {
        val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        return TestViewModelFactory.create(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            savedStateHandle = savedStateHandle,
        ) { linkHandler, thisSavedStateHandle ->
            PaymentSheetViewModel(
                args = args,
                eventReporter = eventReporter,
                paymentElementLoader = paymentElementLoader,
                customerRepository = customerRepository,
                prefsRepository = prefsRepository,
                logger = Logger.noop(),
                workContext = testDispatcher,
                savedStateHandle = thisSavedStateHandle,
                linkHandler = linkHandler,
                confirmationHandlerFactory = createTestConfirmationHandlerFactory(
                    intentConfirmationInterceptor = intentConfirmationInterceptor,
                    savedStateHandle = thisSavedStateHandle,
                    bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
                    stripePaymentLauncherAssistedFactory = paymentLauncherFactory,
                    googlePayPaymentMethodLauncherFactory = googlePayLauncherFactory,
                    paymentConfiguration = paymentConfiguration,
                    statusBarColor = args.statusBarColor,
                    errorReporter = FakeErrorReporter(),
                    linkLauncher = linkPaymentLauncher,
                    cvcRecollectionLauncherFactory = RecordingCvcRecollectionLauncherFactory.noOp(),
                ),
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                errorReporter = errorReporter,
                cvcRecollectionHandler = cvcRecollectionHandler,
                cvcRecollectionInteractorFactory = object : CvcRecollectionInteractor.Factory {
                    override fun create(
                        args: Args,
                        processing: StateFlow<Boolean>,
                        coroutineScope: CoroutineScope,
                    ): CvcRecollectionInteractor {
                        return cvcRecollectionInteractor
                    }
                }
            ).apply {
                if (shouldRegister) {
                    val activityResultCaller = mock<ActivityResultCaller> {
                        on {
                            registerForActivityResult<PaymentLauncherContract.Args, InternalPaymentResult>(any(), any())
                        } doReturn mock()
                    }

                    registerFromActivity(activityResultCaller, TestLifecycleOwner())
                }
            }
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
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
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
            label = "Test".resolvableString,
            iconResource = 0,
            paymentMethodCreateParams = PaymentMethodCreateParams.create(
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

    private fun selectionCvc(selection: PaymentSelection?): String? {
        val saved = (selection as? PaymentSelection.Saved) ?: return null
        val card = (saved.paymentMethodOptionsParams as? PaymentMethodOptionsParams.Card) ?: return null
        return card.cvc
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

    private fun PaymentSheetViewModel.captureGooglePayListener():
        ActivityResultCallback<GooglePayPaymentMethodLauncher.Result> {
        val mockActivityResultCaller = mock<ActivityResultCaller> {
            on {
                registerForActivityResult<
                    GooglePayPaymentMethodLauncherContractV2.Args,
                    GooglePayPaymentMethodLauncher.Result
                    >(any(), any())
            } doReturn mock()
        }

        registerFromActivity(mockActivityResultCaller, TestLifecycleOwner())

        val googlePayListenerCaptor =
            argumentCaptor<ActivityResultCallback<GooglePayPaymentMethodLauncher.Result>>()

        verify(mockActivityResultCaller).registerForActivityResult(
            any<GooglePayPaymentMethodLauncherContractV2>(),
            googlePayListenerCaptor.capture(),
        )

        return googlePayListenerCaptor.firstValue
    }

    private fun getPaymentMethodOptionJsonStringWithCvcRecollectionValue(enabled: Boolean): String {
        return "{\"card\":{\"require_cvc_recollection\":$enabled}}"
    }

    private val BaseSheetViewModel.supportedPaymentMethodTypes: List<String>
        get() = paymentMethodMetadata.value?.supportedPaymentMethodTypes().orEmpty()

    private companion object {
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP =
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY

        private val PAYMENT_METHODS = listOf(CARD_PAYMENT_METHOD)

        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val PAYMENT_INTENT_WITH_PAYMENT_METHOD = PaymentIntentFixtures.PI_WITH_PAYMENT_METHOD
        val SETUP_INTENT = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD

        private const val BACS_ACCOUNT_NUMBER = "00012345"
        private const val BACS_SORT_CODE = "108800"
        private const val BACS_NAME = "John Doe"
        private const val BACS_EMAIL = "johndoe@email.com"

        private val LINK_CONFIG = TestFactory.LINK_CONFIGURATION
    }
}
