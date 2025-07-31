package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.Address
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.TestFactory
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
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
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.asNew
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.confirmation.intent.InvalidDeferredIntentUsageException
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentelement.confirmation.linkinline.LinkInlineSignupConfirmationOption
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheetFixtures.ARGS_DEFERRED_INTENT
import com.stripe.android.paymentsheet.PaymentSheetFixtures.BILLING_DETAILS_FORM_DETAILS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
import com.stripe.android.paymentsheet.PaymentSheetFixtures.PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.addresselement.AutocompleteContract
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.analytics.primaryButtonColorUsage
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.FakeCvcRecollectionHandler
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.PaymentSheetViewState.UserErrorMessage
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
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
import com.stripe.android.paymentsheet.ui.cardParamsUpdateAction
import com.stripe.android.paymentsheet.utils.LinkTestUtils
import com.stripe.android.paymentsheet.utils.prefillCreate
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.ResetMockRule
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.SessionTestRule
import com.stripe.android.ui.core.Amount
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.BankFormScreenStateFactory
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import com.stripe.android.utils.PaymentElementCallbackTestRule
import com.stripe.android.utils.RelayingPaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
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
@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class PaymentSheetViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val prefsRepository = FakePrefsRepository()

    private val cvcRecollectionHandler = FakeCvcRecollectionHandler()

    private val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator()

    @get:Rule
    val rule = RuleChain.emptyRuleChain()
        .around(InstantTaskExecutorRule())
        .around(SessionTestRule())
        .around(PaymentElementCallbackTestRule())
        .around(ResetMockRule(eventReporter))
        .around(RetryRule(3))

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
        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        verify(eventReporter).onInit(
            commonConfiguration = eq(config.asCommonConfiguration()),
            appearance = eq(config.appearance),
            primaryButtonColor = eq(config.primaryButtonColorUsage()),
            configurationSpecificPayload = eq(PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config)),
            isDeferred = eq(false),
        )

        // Creating the view model should regenerate the analytics sessionId.
        assertThat(beforeSessionId).isNotEqualTo(AnalyticsRequestFactory.sessionId)
    }

    @Test
    @Suppress("LongMethod")
    fun `modifyPaymentMethod should use loaded customer info when modifying payment methods`() = runTest {
        Dispatchers.setMain(testDispatcher)
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .customer(
                        CustomerConfiguration(
                            id = "cus_1",
                            ephemeralKeySecret = "ek_123"
                        )
                    )
                    .build()
            ),
            customer = CustomerState(
                id = "cus_2",
                ephemeralKeySecret = "ek_123",
                customerSessionClientSecret = null,
                paymentMethods = paymentMethods,
                defaultPaymentMethodId = null,
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

                interactor.cardParamsUpdateAction(CardBrand.Visa)

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
        Dispatchers.setMain(testDispatcher)
        val eventReporter = FakeEventReporter()
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
            customerRepository = customerRepository,
            eventReporter = eventReporter
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

                interactor.cardParamsUpdateAction(
                    cardBrand = CardBrand.Visa,
                    expiryMonth = 12,
                    expiryYear = 2027,
                    billingDetails = BILLING_DETAILS_FORM_DETAILS
                )

                interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)
            }

            assertThat(awaitItem()).isInstanceOf<SelectSavedPaymentMethods>()
        }

        val updatePaymentMethodSucceededCall = eventReporter.updatePaymentMethodSucceededCalls.awaitItem()
        eventReporter.updatePaymentMethodSucceededCalls.ensureAllEventsConsumed()
        assertThat(updatePaymentMethodSucceededCall.selectedBrand).isEqualTo(CardBrand.Visa)

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
                ),
                expiryMonth = 12,
                expiryYear = 2027,
                billingDetails = BILLING_DETAILS_FORM_DETAILS
            ).toParamMap()
        )

        assertThat(viewModel.customerStateHolder.paymentMethods.value).isEqualTo(
            listOf(updatedPaymentMethod) + paymentMethods.takeLast(4)
        )
    }

    @Test
    fun `modifyPaymentMethod sends event on failed update`() = runTest {
        Dispatchers.setMain(testDispatcher)
        val eventReporter = FakeEventReporter()
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
            customerRepository = customerRepository,
            eventReporter = eventReporter
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

                interactor.cardParamsUpdateAction(CardBrand.Visa)

                interactor.handleViewAction(UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed)
            }
        }

        val onUpdatePaymentMethodFailedCall = eventReporter.updatePaymentMethodFailedCalls.awaitItem()
        eventReporter.updatePaymentMethodFailedCalls.ensureAllEventsConsumed()
        assertThat(onUpdatePaymentMethodFailedCall.selectedBrand).isEqualTo(CardBrand.Visa)
        assertThat(onUpdatePaymentMethodFailedCall.error.message).isEqualTo("No network found!")
    }

    @Test
    fun `checkout() should confirm saved card payment methods`() = confirmationTest {
        val stripeIntent = PAYMENT_INTENT
        val viewModel = createViewModel(
            stripeIntent = stripeIntent,
        )

        val optionsParams = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
        )

        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = CARD_PAYMENT_METHOD,
            paymentMethodOptionsParams = optionsParams,
        )
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = CARD_PAYMENT_METHOD,
                optionsParams = optionsParams,
                originatedFromWallet = false,
            )
        )
        assertThat(arguments.intent).isEqualTo(stripeIntent)
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

            viewModel.updateSelection(PaymentSelection.Link())
            viewModel.checkout()

            assertThat(errorReporter.getLoggedErrors()).contains(
                "unexpected_error.paymentsheet.invalid_payment_selection"
            )

            assertThat(awaitItem()).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        }
    }

    @Test
    fun `checkout() should confirm saved us_bank_account payment methods`() = confirmationTest {
        val stripeIntent = PAYMENT_INTENT
        val viewModel = createViewModel(
            stripeIntent = stripeIntent,
        )

        val optionsParams = PaymentMethodOptionsParams.USBankAccount(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )

        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT,
            paymentMethodOptionsParams = optionsParams,
        )

        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT,
                optionsParams = optionsParams,
                originatedFromWallet = false,
            )
        )
        assertThat(arguments.intent).isEqualTo(stripeIntent)
    }

    @Test
    fun `checkout() for Setup Intent with saved payment method that requires mandate should include mandate`() =
        confirmationTest {
            val stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
            val viewModel = createViewModel(
                stripeIntent = stripeIntent,
                args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
                    initializationMode = InitializationMode.SetupIntent(stripeIntent.clientSecret!!),
                ),
            )

            val paymentSelection = PaymentSelection.Saved(SEPA_DEBIT_PAYMENT_METHOD)
            viewModel.updateSelection(paymentSelection)
            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = SEPA_DEBIT_PAYMENT_METHOD,
                    optionsParams = null,
                    originatedFromWallet = false,
                )
            )
            assertThat(arguments.intent).isEqualTo(stripeIntent)
        }

    @Test
    fun `checkout() should confirm new payment methods`() = confirmationTest {
        val stripeIntent = PAYMENT_INTENT
        val viewModel = createViewModel(
            stripeIntent = stripeIntent,
        )

        val paymentSelection = PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ),
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )

        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                ),
                extraParams = null,
                shouldSave = true,
            )
        )
        assertThat(arguments.intent).isEqualTo(stripeIntent)
    }

    @Test
    fun `checkout() with shipping should confirm new payment methods`() = confirmationTest {
        val stripeIntent = PAYMENT_INTENT
        val shippingAddress = AddressDetails(
            address = Address(
                country = "US"
            ),
            name = "Test Name"
        )
        val viewModel = createViewModel(
            stripeIntent = stripeIntent,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .shippingDetails(shippingAddress)
                    .build()
            ),
        )

        val optionsParams = PaymentMethodOptionsParams.Card(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )

        val paymentSelection = PaymentSelection.New.Card(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            paymentMethodOptionsParams = optionsParams,
            brand = CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )

        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = optionsParams,
                extraParams = null,
                shouldSave = true,
            )
        )
        assertThat(arguments.intent).isEqualTo(stripeIntent)
        assertThat(arguments.shippingDetails).isEqualTo(shippingAddress)
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
    fun `Link Express is launched when viewmodel is started with logged in link account`() = confirmationTest {
        createViewModel(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null,
            ),
        )

        val confirmationArgs = startTurbine.awaitItem()
        assertThat(confirmationArgs.confirmationOption).isInstanceOf<LinkConfirmationOption>()
        val confirmationOption = confirmationArgs.confirmationOption as? LinkConfirmationOption
        assertThat(confirmationOption?.useLinkExpress).isTrue()
    }

    @Test
    fun `Link Express is not launched when viewmodel is started with logged out link account`() = confirmationTest {
        createViewModel(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        )
    }

    @Test
    fun `checkout with Link starts confirmation with correct arguments`() = confirmationTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        )

        startTurbine.ensureAllEventsConsumed()

        viewModel.checkoutWithLink()

        val confirmationArgs = startTurbine.awaitItem()
        assertThat(confirmationArgs.confirmationOption).isInstanceOf<LinkConfirmationOption>()
        val confirmationOption = confirmationArgs.confirmationOption as? LinkConfirmationOption
        assertThat(confirmationOption?.useLinkExpress).isFalse()
    }

    @Test
    fun `Google Pay checkout cancelled returns to Idle state`() = confirmationTest {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

        confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val processingTurbine = viewModel.processing.testIn(this)
            val contentVisibleTurbine = viewModel.contentVisible.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Processing)
            assertThat(processingTurbine.awaitItem()).isTrue()
            assertThat(contentVisibleTurbine.awaitItem()).isFalse()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.None,
                )
            )

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Idle(null))
            assertThat(processingTurbine.awaitItem()).isFalse()
            assertThat(contentVisibleTurbine.awaitItem()).isTrue()

            walletsProcessingStateTurbine.cancel()
            processingTurbine.cancel()
            contentVisibleTurbine.cancel()
        }
    }

    @Test
    fun `On checkout clear the previous view state error`() = confirmationTest {
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

            val arguments = startTurbine.awaitItem()

            confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(null)
            assertThat(buyButtonTurbine.awaitItem())
                .isEqualTo(PaymentSheetViewState.StartProcessing)

            walletsProcessingStateTurbine.cancel()
            buyButtonTurbine.cancel()
        }
    }

    @Test
    fun `Google Pay checkout failed returns to Idle state and shows error`() = confirmationTest {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

        confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val processingTurbine = viewModel.processing.testIn(this)
            val contentVisibleTurbine = viewModel.contentVisible.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Processing)
            assertThat(processingTurbine.awaitItem()).isTrue()
            assertThat(contentVisibleTurbine.awaitItem()).isFalse()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = Exception("Test exception"),
                    message = PaymentsCoreR.string.stripe_internal_error.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            )

            assertThat(contentVisibleTurbine.awaitItem()).isTrue()
            assertThat(walletsProcessingStateTurbine.awaitItem())
                .isEqualTo(WalletsProcessingState.Idle(PaymentsCoreR.string.stripe_internal_error.resolvableString))
            assertThat(processingTurbine.awaitItem()).isFalse()

            contentVisibleTurbine.cancel()
            walletsProcessingStateTurbine.cancel()
            processingTurbine.cancel()
        }
    }

    @Test
    fun `On inline link payment, should process with primary button`() = confirmationTest {
        val linkConfiguration = LinkTestUtils.createLinkConfiguration()
        val signupMode = LinkSignupMode.InsteadOfSaveForFutureUse

        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = linkConfiguration,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = signupMode,
            ),
        )

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val buyButtonStateTurbine = viewModel.buyButtonState.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(null)
            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(
                PaymentSheetViewState.Reset(null)
            )

            val linkInlineHandler = LinkInlineHandler.create()
            val formHelper = DefaultFormHelper.create(
                viewModel = viewModel,
                paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
                linkInlineHandler = linkInlineHandler,
            )

            formHelper.onFormFieldValuesChanged(
                formValues = FormFieldValues(
                    fieldValuePairs = mapOf(
                        IdentifierSpec.CardBrand to FormFieldEntry(CardBrand.Visa.code, true),
                    ),
                    userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                ),
                selectedPaymentMethodCode = "card",
            )

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

            val arguments = startTurbine.awaitItem()

            confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

            walletsProcessingStateTurbine.expectNoEvents()

            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(PaymentSheetViewState.StartProcessing)

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = null,
                )
            )

            assertThat(buyButtonStateTurbine.awaitItem()).isInstanceOf<PaymentSheetViewState.FinishProcessing>()

            buyButtonStateTurbine.cancel()
            walletsProcessingStateTurbine.cancel()
        }
    }

    @Test
    fun `On inline link payment with save requested, should set 'paymentMethodOptionsParams' SFU to off_session`() =
        confirmationTest {
            val viewModel = createLinkViewModel()

            viewModel.updateSelection(
                createLinkInlinePaymentSelection(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                    input = UserInput.SignUp(
                        email = "email@email.com",
                        phone = "+12267007611",
                        country = "CA",
                        name = "John Doe",
                        consentAction = SignUpConsentAction.Checkbox,
                    ),
                )
            )

            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isInstanceOf<LinkInlineSignupConfirmationOption>()

            val inlineOption = arguments.confirmationOption as LinkInlineSignupConfirmationOption

            assertThat(inlineOption.saveOption).isEqualTo(
                LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.RequestedReuse
            )
        }

    @Test
    fun `On inline link payment with save not requested, should set 'paymentMethodOptionsParams' SFU to blank`() =
        confirmationTest {
            val viewModel = createLinkViewModel()

            viewModel.updateSelection(
                createLinkInlinePaymentSelection(
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
                    input = UserInput.SignUp(
                        email = "email@email.com",
                        phone = "+12267007611",
                        country = "CA",
                        name = "John Doe",
                        consentAction = SignUpConsentAction.Checkbox,
                    ),
                )
            )

            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isInstanceOf<LinkInlineSignupConfirmationOption>()

            val inlineOption = arguments.confirmationOption as LinkInlineSignupConfirmationOption

            assertThat(inlineOption.saveOption).isEqualTo(
                LinkInlineSignupConfirmationOption.PaymentMethodSaveOption.NoRequest
            )
        }

    @Test
    fun `On link payment through launcher, should process with wallets processing state`() = confirmationTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
            ),
        )

        turbineScope {
            val walletsProcessingStateTurbine = viewModel.walletsProcessingState.testIn(this)
            val buyButtonStateTurbine = viewModel.buyButtonState.testIn(this)

            assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(null)
            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(
                PaymentSheetViewState.Reset(null)
            )

            viewModel.checkoutWithLink()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                LinkConfirmationOption(
                    useLinkExpress = false,
                    configuration = TestFactory.LINK_CONFIGURATION,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

            assertThat(walletsProcessingStateTurbine.awaitItem()).isEqualTo(WalletsProcessingState.Processing)
            assertThat(buyButtonStateTurbine.awaitItem()).isEqualTo(null)

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = null,
                )
            )

            assertThat(walletsProcessingStateTurbine.awaitItem()).isInstanceOf<WalletsProcessingState.Completed>()

            buyButtonStateTurbine.cancel()
            walletsProcessingStateTurbine.cancel()
        }
    }

    @Test
    fun `On confirmation result, should update ViewState and save preferences`() = confirmationTest {
        val viewModel = createViewModel()

        val selection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        viewModel.updateSelection(selection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = CARD_PAYMENT_METHOD,
                optionsParams = null,
            )
        )

        turbineScope {
            val resultTurbine = viewModel.paymentSheetResult.testIn(this)
            val viewStateTurbine = viewModel.viewState.testIn(this)

            confirmationState.value = ConfirmationHandler.State.Complete(
                result = ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = null,
                )
            )

            assertThat(viewStateTurbine.awaitItem())
                .isEqualTo(PaymentSheetViewState.Reset(null))

            val finishedProcessingState = viewStateTurbine.awaitItem()
            assertThat(finishedProcessingState)
                .isInstanceOf<PaymentSheetViewState.FinishProcessing>()

            (finishedProcessingState as PaymentSheetViewState.FinishProcessing).onComplete()

            assertThat(resultTurbine.awaitItem())
                .isEqualTo(PaymentSheetResult.Completed())

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
    fun `On confirmation result, should update ViewState and not save new payment method`() = confirmationTest {
        val viewModel = createViewModel(
            stripeIntent = PAYMENT_INTENT,
        )

        val selection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        viewModel.updateSelection(selection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = true,
            )
        )

        turbineScope {
            val resultTurbine = viewModel.paymentSheetResult.testIn(this)
            val viewStateTurbine = viewModel.viewState.testIn(this)

            confirmationState.value = ConfirmationHandler.State.Complete(
                result = ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = null,
                )
            )

            assertThat(viewStateTurbine.awaitItem())
                .isEqualTo(PaymentSheetViewState.Reset(null))

            val finishedProcessingState = viewStateTurbine.awaitItem()
            assertThat(finishedProcessingState)
                .isInstanceOf<PaymentSheetViewState.FinishProcessing>()

            (finishedProcessingState as PaymentSheetViewState.FinishProcessing).onComplete()

            assertThat(resultTurbine.awaitItem()).isEqualTo(PaymentSheetResult.Completed())

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
    fun `On confirmation result, with non-success outcome should report failure event`() = confirmationTest {
        val viewModel = createViewModel()
        val selection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        val error = APIException()

        viewModel.updateSelection(selection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = CARD_PAYMENT_METHOD,
                optionsParams = null,
            )
        )

        viewModel.paymentMethodMetadata.test {
            confirmationState.value = ConfirmationHandler.State.Complete(
                result = ConfirmationHandler.Result.Failed(
                    cause = error,
                    message = error.errorMessage,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            )
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
    fun `On fail due to invalid deferred intent usage, should report with expected integration error`() =
        confirmationTest {
            val eventReporter = FakeEventReporter()
            val viewModel = createViewModel(
                eventReporter = eventReporter,
            )

            viewModel.updateSelection(CARD_PAYMENT_SELECTION)
            viewModel.checkout()

            assertThat(startTurbine.awaitItem()).isNotNull()

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
    fun `on confirmation result, should update emit generic error on IOExceptions`() = confirmationTest {
        val viewModel = createViewModel()

        viewModel.viewState.test {
            val error = IOException("very helpful error message")

            confirmationState.value = ConfirmationHandler.State.Complete(
                result = ConfirmationHandler.Result.Failed(
                    cause = error,
                    message = R.string.stripe_something_went_wrong.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            )

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
    fun `On confirmation result, should update emit Stripe API errors`() = confirmationTest {
        val viewModel = createViewModel()

        viewModel.viewState.test {
            val errorMessage = "very helpful error message"
            val error = APIException(StripeError(message = errorMessage))

            confirmationState.value = ConfirmationHandler.State.Complete(
                result = ConfirmationHandler.Result.Failed(
                    cause = error,
                    message = errorMessage.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            )

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
    fun `Verify supported payment methods includes afterpay if no shipping and no allow flag`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .shippingDetails(null)
                    .allowsPaymentMethodsRequiringShippingAddress(false)
                    .build()
            ),
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                paymentMethodTypes = listOf("afterpay_clearpay"),
                shipping = null,
            ),
        )

        assertThat(viewModel.supportedPaymentMethodTypes).containsExactly("afterpay_clearpay")
    }

    @Test
    fun `Verify supported payment methods include afterpay if allow flag but no shipping`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .allowsPaymentMethodsRequiringShippingAddress(true)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .allowsPaymentMethodsRequiringShippingAddress(true)
                    .build()
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
            GooglePayConfiguration.ButtonType.Plain,
            GooglePayButtonType.Plain
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            GooglePayConfiguration.ButtonType.Pay,
            GooglePayButtonType.Pay
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            GooglePayConfiguration.ButtonType.Book,
            GooglePayButtonType.Book
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            GooglePayConfiguration.ButtonType.Buy,
            GooglePayButtonType.Buy
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            GooglePayConfiguration.ButtonType.Donate,
            GooglePayButtonType.Donate
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            GooglePayConfiguration.ButtonType.Checkout,
            GooglePayButtonType.Checkout
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            GooglePayConfiguration.ButtonType.Order,
            GooglePayButtonType.Order
        )

        testButtonTypeParsedToProperGooglePayButtonType(
            GooglePayConfiguration.ButtonType.Subscribe,
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
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = PaymentSheet.Configuration(
                    merchantDisplayName = "Example, Inc.",
                    allowsDelayedPaymentMethods = false
                )
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
    fun `Resets selection correctly after cancelling Google Pay`() = confirmationTest {
        val viewModel = createViewModel(
            initialPaymentSelection = null,
        )

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

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.None,
                )
            )

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
    fun `Content should be hidden when Google Pay is visible`() = confirmationTest {
        val viewModel = createViewModel()

        viewModel.contentVisible.test {
            assertThat(awaitItem()).isTrue()
            viewModel.checkoutWithGooglePay()

            val arguments = startTurbine.awaitItem()

            confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

            assertThat(awaitItem()).isFalse()

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Canceled(
                    action = ConfirmationHandler.Result.Canceled.Action.None,
                )
            )

            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Content should be hidden when Link is visible`() = confirmationTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        )

        viewModel.contentVisible.test {
            assertThat(awaitItem()).isTrue()

            viewModel.checkoutWithLink()

            val arguments = startTurbine.awaitItem()

            confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `Does not show processing WalletsProcessingState when using Link Express`() = confirmationTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.NeedsVerification,
                signupMode = null,
            ),
        )

        viewModel.walletsProcessingState.test {
            assertThat(awaitItem()).isNull()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                LinkConfirmationOption(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    useLinkExpress = true,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

            assertThat(awaitItem()).isEqualTo(WalletsProcessingState.Idle(error = null))
        }
    }

    @Test
    fun `launched with correct screen when in horizontal mode`() = runTest {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Automatic)
                    .build()
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
                assertThat(awaitItem()).isEqualTo(PaymentSheetResult.Canceled())
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
                billingDetails = BillingDetails(),
            )
        )
    }

    @Test
    fun `On load with initial Google Pay selection, selection should be null & primary button disabled`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            initialPaymentSelection = PaymentSelection.GooglePay,
        )

        viewModel.selection.test {
            assertThat(awaitItem()).isNull()
        }

        viewModel.primaryButtonUiState.test {
            val uiState = awaitItem()

            assertThat(uiState).isNotNull()
            assertThat(uiState?.enabled).isFalse()
        }
    }

    @Test
    fun `On load with initial Link selection, selection should be null & primary button disabled`() = runTest {
        val viewModel = createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            initialPaymentSelection = PaymentSelection.GooglePay,
        )

        viewModel.selection.test {
            assertThat(awaitItem()).isNull()
        }

        viewModel.primaryButtonUiState.test {
            val uiState = awaitItem()

            assertThat(uiState).isNotNull()
            assertThat(uiState?.enabled).isFalse()
        }
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
            customerRepository = FakeCustomerRepository(PAYMENT_METHODS),
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
                linkAttestationCheck = FakeLinkAttestationCheck().apply {
                    result = LinkAttestationCheck.Result.AccountError(
                        error = Exception("Cannot attest!")
                    )
                }
            )
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
            customerRepository = FakeCustomerRepository(PAYMENT_METHODS),
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
                linkAttestationCheck = FakeLinkAttestationCheck().apply {
                    result = LinkAttestationCheck.Result.AccountError(
                        error = Exception("Cannot attest!")
                    )
                }
            )
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
                NewPaymentOptionSelection.New(
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
                NewPaymentOptionSelection.External(
                    newSelection
                )
            )
        }
    }

    @Test
    fun `updateSelection with custom payment method updates the current selection`() = runTest {
        val viewModel = createViewModel(initialPaymentSelection = null)

        viewModel.selection.test {
            val newSelection = PaymentSelection.CustomPaymentMethod(
                id = "cpmt_1",
                billingDetails = null,
                label = "BufoPay".resolvableString,
                lightThemeIconUrl = "some_url",
                darkThemeIconUrl = "some_url",
            )
            assertThat(awaitItem()).isNull()
            viewModel.updateSelection(newSelection)
            assertThat(awaitItem()).isEqualTo(newSelection)
            assertThat(viewModel.newPaymentSelection).isEqualTo(
                NewPaymentOptionSelection.Custom(
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .allowsDelayedPaymentMethods(true)
                    .build(),
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .allowsDelayedPaymentMethods(true)
                    .build(),
            ),
            stripeIntent = intent,
        )

        viewModel.walletsState.test {
            val textResource = awaitItem()?.dividerTextResource
            assertThat(textResource).isEqualTo(R.string.stripe_paymentsheet_or_use)
        }
    }

    @Test
    fun `Completes if confirmation handler returns a successful result`() = confirmationTest {
        val viewModel = createViewModelForDeferredIntent()

        viewModel.paymentSheetResult.test {
            val savedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
            viewModel.updateSelection(savedSelection)
            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD,
                    optionsParams = null,
                    originatedFromWallet = false,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = null,
                )
            )

            val finishingState = viewModel.viewState.value as PaymentSheetViewState.FinishProcessing
            finishingState.onComplete()

            assertThat(awaitItem()).isEqualTo(PaymentSheetResult.Completed())
        }
    }

    @Test
    fun `Displays failure if confirmation handler returns a failure`() = confirmationTest {
        val viewModel = createViewModelForDeferredIntent()

        val paymentMethod = CARD_PAYMENT_METHOD
        val error = "oh boy this didn't work"

        viewModel.viewState.test {
            assertThat(awaitItem()).isEqualTo(PaymentSheetViewState.Reset())

            val savedSelection = PaymentSelection.Saved(paymentMethod)
            viewModel.updateSelection(savedSelection)
            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD,
                    optionsParams = null,
                    originatedFromWallet = false,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

            assertThat(awaitItem()).isEqualTo(PaymentSheetViewState.StartProcessing)

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Failed(
                    cause = Exception(error),
                    message = error.resolvableString,
                    type = ConfirmationHandler.Result.Failed.ErrorType.Payment,
                )
            )

            assertThat(awaitItem()).isEqualTo(PaymentSheetViewState.Reset(UserErrorMessage(error.resolvableString)))
        }
    }

    @Test
    fun `Sends correct analytics event when using normal intent`() = runTest {
        createViewModel()

        verify(eventReporter).onInit(
            commonConfiguration = anyOrNull(),
            appearance = anyOrNull(),
            primaryButtonColor = anyOrNull(),
            configurationSpecificPayload = any(),
            isDeferred = eq(false),
        )
    }

    @Test
    fun `Sends correct analytics event when using deferred intent with client-side confirmation`() = runTest {
        PaymentElementCallbackReferences[PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ ->
                error("Should not be called!")
            }
            .confirmCustomPaymentMethodCallback { _, _ ->
                error("Should not be called!")
            }
            .externalPaymentMethodConfirmHandler { _, _ ->
                error("Should not be called!")
            }
            .build()

        createViewModelForDeferredIntent()

        verify(eventReporter).onInit(
            commonConfiguration = anyOrNull(),
            appearance = anyOrNull(),
            primaryButtonColor = anyOrNull(),
            configurationSpecificPayload = any(),
            isDeferred = eq(true),
        )
    }

    @Test
    fun `Sends correct analytics event when using deferred intent with server-side confirmation`() = runTest {
        PaymentElementCallbackReferences[PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ ->
                error("Should not be called!")
            }
            .confirmCustomPaymentMethodCallback { _, _ ->
                error("Should not be called!")
            }
            .externalPaymentMethodConfirmHandler { _, _ ->
                error("Should not be called!")
            }
            .build()

        createViewModelForDeferredIntent()

        verify(eventReporter).onInit(
            commonConfiguration = anyOrNull(),
            appearance = anyOrNull(),
            primaryButtonColor = anyOrNull(),
            configurationSpecificPayload = any(),
            isDeferred = eq(true),
        )
    }

    @Test
    fun `Sends no deferred_intent_confirmation_type for non-deferred intent confirmation`() = confirmationTest {
        val viewModel = createViewModel()

        val paymentMethod = CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)

        viewModel.updateSelection(savedSelection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            result = ConfirmationHandler.Result.Succeeded(
                intent = PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
            )
        )

        verify(eventReporter).onPaymentSuccess(
            paymentSelection = eq(savedSelection),
            deferredIntentConfirmationType = isNull(),
        )
    }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for none confirmation type of deferred intent`() =
        confirmationTest {
            val viewModel = createViewModelForDeferredIntent()

            val paymentMethod = CARD_PAYMENT_METHOD
            val savedSelection = PaymentSelection.Saved(paymentMethod)

            viewModel.updateSelection(savedSelection)
            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD,
                    optionsParams = null,
                    originatedFromWallet = false,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.None,
                )
            )

            verify(eventReporter).onPaymentSuccess(
                paymentSelection = eq(savedSelection),
                deferredIntentConfirmationType = eq(DeferredIntentConfirmationType.None),
            )
        }

    @Test
    fun `Sends correct deferred_intent_confirmation_type for client-side confirmation of deferred intent`() =
        confirmationTest {
            val viewModel = createViewModelForDeferredIntent()

            val paymentMethod = CARD_PAYMENT_METHOD
            val savedSelection = PaymentSelection.Saved(paymentMethod)

            viewModel.updateSelection(savedSelection)
            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD,
                    optionsParams = null,
                    originatedFromWallet = false,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
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
            val viewModel = createViewModelForDeferredIntent()

            val paymentMethod = CARD_PAYMENT_METHOD
            val savedSelection = PaymentSelection.Saved(paymentMethod)

            viewModel.updateSelection(savedSelection)
            viewModel.checkout()

            val arguments = startTurbine.awaitItem()

            assertThat(arguments.confirmationOption).isEqualTo(
                PaymentMethodConfirmationOption.Saved(
                    paymentMethod = CARD_PAYMENT_METHOD,
                    optionsParams = null,
                    originatedFromWallet = false,
                )
            )

            confirmationState.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PAYMENT_INTENT,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
                )
            )

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
            instantDebits = null,
            screenState = BankFormScreenStateFactory.createWithSession("session_1234"),
        )
        viewModel.updateSelection(usBankAccount)

        viewModel.checkout()

        verify(eventReporter, never()).onPressConfirmButton(any())
    }

    @Test
    fun `Launches Google Pay with custom label if provided for payment intent`() = confirmationTest {
        val expectedLabel = "My custom label"
        val expectedAmount = 12345L

        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "CA",
                        currencyCode = "CAD",
                        amount = expectedAmount,
                        label = expectedLabel,
                    )
                )
                .build()
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
        )

        viewModel.checkoutWithGooglePay()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

        val googlePayConfirmationOption = arguments.confirmationOption as GooglePayConfirmationOption

        assertThat(googlePayConfirmationOption.config.customLabel).isEqualTo(expectedLabel)
        assertThat(googlePayConfirmationOption.config.customAmount).isEqualTo(expectedAmount)
    }

    @Test
    fun `Launches Google Pay with custom label and amount if provided for setup intent`() = confirmationTest {
        val expectedLabel = "My custom label"
        val expectedAmount = 1234L

        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.newBuilder()
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "CA",
                        currencyCode = "CAD",
                        amount = expectedAmount,
                        label = expectedLabel,
                    )
                )
                .build()
        )

        val viewModel = createViewModel(
            args = args,
            isGooglePayReady = true,
            stripeIntent = SETUP_INTENT,
        )

        viewModel.checkoutWithGooglePay()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isInstanceOf<GooglePayConfirmationOption>()

        val googlePayConfirmationOption = arguments.confirmationOption as GooglePayConfirmationOption

        assertThat(googlePayConfirmationOption.config.customLabel).isEqualTo(expectedLabel)
        assertThat(googlePayConfirmationOption.config.customAmount).isEqualTo(expectedAmount)
    }

    @Test
    fun `Start confirmation with Bacs debit selected and filled`() = confirmationTest {
        val viewModel = createViewModel()
        val bacsPaymentSelection = createBacsPaymentSelection()

        viewModel.updateSelection(bacsPaymentSelection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            BacsConfirmationOption(
                createParams = bacsPaymentSelection.paymentMethodCreateParams,
                optionsParams = bacsPaymentSelection.paymentMethodOptionsParams,
            )
        )
    }

    @Test
    fun `Requires email and phone with Google Pay when collection mode is set to always`() {
        val args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.newBuilder()
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "CA",
                        currencyCode = "CAD",
                    )
                )
                .billingDetailsCollectionConfiguration(
                    BillingDetailsCollectionConfiguration(
                        phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                        email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
                    )
                )
                .build()
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
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.newBuilder()
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "CA",
                        currencyCode = "CAD",
                    )
                )
                .billingDetailsCollectionConfiguration(
                    BillingDetailsCollectionConfiguration(
                        address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                    )
                )
                .build()
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
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.newBuilder()
                .billingDetailsCollectionConfiguration(
                    BillingDetailsCollectionConfiguration(
                        phone = BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                        email = BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                    )
                )
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "CA",
                        currencyCode = "CAD",
                    )
                )
                .build()
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
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.newBuilder()
                .billingDetailsCollectionConfiguration(
                    BillingDetailsCollectionConfiguration(
                        address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                    )
                )
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "CA",
                        currencyCode = "CAD",
                    )
                )
                .build()
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
    fun `On checkout with Google Pay, should report success as expected`() = confirmationTest {
        val eventReporter = FakeEventReporter()
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
            isGooglePayReady = true,
            eventReporter = eventReporter,
        )

        viewModel.checkoutWithGooglePay()

        val config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config
        val googlePayConfig = ARGS_CUSTOMER_WITH_GOOGLEPAY.googlePayConfig!!

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            GooglePayConfirmationOption(
                config = GooglePayConfirmationOption.Config(
                    environment = googlePayConfig.environment,
                    customLabel = googlePayConfig.label,
                    customAmount = googlePayConfig.amount,
                    merchantName = config.merchantDisplayName,
                    merchantCountryCode = googlePayConfig.countryCode,
                    merchantCurrencyCode = googlePayConfig.currencyCode,
                    billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
                    cardBrandFilter = PaymentSheetCardBrandFilter(config.cardBrandAcceptance),
                )
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        val paymentSuccessCall = eventReporter.paymentSuccessCalls.awaitItem()

        assertThat(paymentSuccessCall.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `On checkout with Google Pay, should report failure as expected`() = confirmationTest {
        val eventReporter = FakeEventReporter()
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
            isGooglePayReady = true,
            eventReporter = eventReporter,
        )

        viewModel.checkoutWithGooglePay()

        val config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config
        val googlePayConfig = ARGS_CUSTOMER_WITH_GOOGLEPAY.googlePayConfig!!

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            GooglePayConfirmationOption(
                config = GooglePayConfirmationOption.Config(
                    environment = googlePayConfig.environment,
                    customLabel = googlePayConfig.label,
                    customAmount = googlePayConfig.amount,
                    merchantName = config.merchantDisplayName,
                    merchantCountryCode = googlePayConfig.countryCode,
                    merchantCurrencyCode = googlePayConfig.currencyCode,
                    billingDetailsCollectionConfiguration = config.billingDetailsCollectionConfiguration,
                    cardBrandFilter = PaymentSheetCardBrandFilter(config.cardBrandAcceptance),
                )
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("This failed!"),
                message = "This failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(errorCode = 10),
            )
        )

        val paymentFailureCall = eventReporter.paymentFailureCalls.awaitItem()

        assertThat(paymentFailureCall.paymentSelection).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `On checkout with Link, should report success as expected`() = confirmationTest {
        val eventReporter = FakeEventReporter()
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
            linkState = LinkState(
                configuration = LINK_CONFIG,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null
            ),
            eventReporter = eventReporter,
        )

        viewModel.checkoutWithLink()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            LinkConfirmationOption(
                useLinkExpress = false,
                configuration = LINK_CONFIG,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        val paymentSuccessCall = eventReporter.paymentSuccessCalls.awaitItem()

        assertThat(paymentSuccessCall.paymentSelection).isEqualTo(PaymentSelection.Link(useLinkExpress = false))
    }

    @Test
    fun `On checkout with Link, should report failure as expected`() = confirmationTest {
        val eventReporter = FakeEventReporter()
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
            linkState = LinkState(
                configuration = LINK_CONFIG,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null
            ),
            eventReporter = eventReporter,
        )

        viewModel.checkoutWithLink()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            LinkConfirmationOption(
                useLinkExpress = false,
                configuration = LINK_CONFIG,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("This failed!"),
                message = "This failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(errorCode = 10),
            )
        )

        val paymentFailureCall = eventReporter.paymentFailureCalls.awaitItem()

        assertThat(paymentFailureCall.paymentSelection).isEqualTo(PaymentSelection.Link(useLinkExpress = false))
    }

    @Test
    fun `On checkout with Link Express, should report success as expected`() = confirmationTest {
        val eventReporter = FakeEventReporter()

        createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
            linkState = LinkState(
                configuration = LINK_CONFIG,
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null
            ),
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            eventReporter = eventReporter,
        )

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            LinkConfirmationOption(
                useLinkExpress = true,
                configuration = LINK_CONFIG,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Succeeded(
                intent = PaymentIntentFixtures.PI_SUCCEEDED,
                deferredIntentConfirmationType = null,
            )
        )

        val paymentSuccessCall = eventReporter.paymentSuccessCalls.awaitItem()

        assertThat(paymentSuccessCall.paymentSelection).isEqualTo(PaymentSelection.Link(useLinkExpress = true))
    }

    @Test
    fun `On checkout with Link Express, should report failure as expected`() = confirmationTest {
        val eventReporter = FakeEventReporter()

        createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
            linkState = LinkState(
                configuration = LINK_CONFIG,
                loginState = LinkState.LoginState.LoggedIn,
                signupMode = null
            ),
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            eventReporter = eventReporter,
        )

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.confirmationOption).isEqualTo(
            LinkConfirmationOption(
                useLinkExpress = true,
                configuration = LINK_CONFIG,
            )
        )

        confirmationState.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = IllegalStateException("This failed!"),
                message = "This failed".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.GooglePay(errorCode = 10),
            )
        )

        val paymentFailureCall = eventReporter.paymentFailureCalls.awaitItem()

        assertThat(paymentFailureCall.paymentSelection).isEqualTo(PaymentSelection.Link(useLinkExpress = true))
    }

    @Test
    fun `Can complete payment after switching to another LPM from card selection with inline Link signup state`() =
        confirmationTest {
            val signupMode = LinkSignupMode.InsteadOfSaveForFutureUse
            val viewModel = createViewModel(
                customer = EMPTY_CUSTOMER_STATE,
                stripeIntent = PAYMENT_INTENT.copy(
                    paymentMethodTypes = listOf("card", "link", "bancontact")
                ),
                linkState = LinkState(LINK_CONFIG, LinkState.LoginState.LoggedOut, signupMode),
            )

            viewModel.primaryButtonUiState.test {
                assertThat(awaitItem()?.enabled).isFalse()

                val linkInlineHandler = LinkInlineHandler.create()
                val formHelper = DefaultFormHelper.create(
                    viewModel = viewModel,
                    paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
                    linkInlineHandler = linkInlineHandler,
                )

                formHelper.onFormFieldValuesChanged(
                    formValues = FormFieldValues(
                        fieldValuePairs = mapOf(
                            IdentifierSpec.CardBrand to FormFieldEntry(CardBrand.Visa.code, true),
                        ),
                        userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                    ),
                    selectedPaymentMethodCode = "card",
                )

                val buyButton = awaitItem()

                assertThat(buyButton?.enabled).isTrue()

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

                expectNoEvents()

                formHelper.onFormFieldValuesChanged(
                    formValues = FormFieldValues(
                        fieldValuePairs = mapOf(
                            IdentifierSpec.Country to FormFieldEntry("CA", true),
                        ),
                        userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
                    ),
                    selectedPaymentMethodCode = "bancontact",
                )

                buyButton?.onClick?.invoke()

                val arguments = startTurbine.awaitItem()

                assertThat(arguments.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.New>()

                val newConfirmationOption = arguments.confirmationOption.asNew()

                assertThat(newConfirmationOption.shouldSave).isFalse()

                confirmationState.value = ConfirmationHandler.State.Confirming(arguments.confirmationOption)

                assertThat(awaitItem()?.enabled).isFalse()
            }
        }

    @Test
    fun `On complete payment launcher result in PI mode & should reuse, should save payment selection`() =
        selectionSavedTest(
            initializationMode = InitializationMode.PaymentIntent(
                clientSecret = "pi_12345"
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )

    @Test
    fun `On complete payment launcher result in PI mode & should not reuse, should not save payment selection`() =
        selectionSavedTest(
            initializationMode = InitializationMode.PaymentIntent(
                clientSecret = "pi_12345"
            ),
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
            shouldSave = false
        )

    @Test
    fun `On complete payment launcher result in SI mode, should save payment selection`() =
        selectionSavedTest(
            initializationMode = InitializationMode.SetupIntent(
                clientSecret = "si_123456"
            )
        )

    @Test
    fun `On complete payment launcher result with PI config but no SFU, should not save payment selection`() =
        selectionSavedTest(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD"
                    )
                )
            ),
            shouldSave = false
        )

    @Test
    fun `On complete payment launcher result with DI (PI+SFU), should save payment selection`() =
        selectionSavedTest(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD",
                        setupFutureUse = IntentConfiguration.SetupFutureUse.OffSession
                    )
                )
            )
        )

    @Test
    fun `On complete payment launcher result with DI (SI), should save payment selection`() =
        selectionSavedTest(
            initializationMode = InitializationMode.DeferredIntent(
                intentConfiguration = IntentConfiguration(
                    mode = IntentConfiguration.Mode.Setup(
                        currency = "USD"
                    )
                )
            )
        )

    @Test
    fun `Returns payment success when confirm state is restored if result is returned before state loads`() =
        testConfirmationStateRestorationAfterPaymentSuccess(loadStateBeforePaymentResult = false)

    @Test
    fun `Returns payment success after process death if state is loaded before result is returned`() =
        testConfirmationStateRestorationAfterPaymentSuccess(loadStateBeforePaymentResult = true)

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
                interactor.cardParamsUpdateAction(CardBrand.Visa)

                verify(customerRepository, never()).updatePaymentMethod(any(), any(), any())
            }
        }
    }

    @Test
    fun `When a CardBrandChoice is disabled, label should be appended with (not accepted)`() {
        // Create a disabled CardBrandChoice
        val choice = CardBrandChoice(
            brand = CardBrand.Visa,
            enabled = false
        )

        val resolvedLabel = choice.label.resolve(ApplicationProvider.getApplicationContext())
        // Verify "(not accepted)" is appended to the display label
        assertThat(resolvedLabel).contains("(not accepted)")
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
    fun `On confirm with existing payment method, calls confirm with expected parameters`() = confirmationTest {
        val initializationMode = InitializationMode.PaymentIntent(clientSecret = "pi_123")
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                initializationMode = initializationMode,
            ),
            stripeIntent = intent,
        )

        val paymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        val arguments = startTurbine.awaitItem()

        assertThat(arguments.initializationMode).isEqualTo(initializationMode)
        assertThat(arguments.confirmationOption).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                paymentMethod = CARD_PAYMENT_METHOD,
                optionsParams = null,
                originatedFromWallet = false,
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Automatic)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Automatic)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                    .build()
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
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Automatic)
                    .build()
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
    fun `On register for activity result, should register confirmation handler & autocomplete launcher`() =
        confirmationTest {
            DummyActivityResultCaller.test {
                val lifecycleOwner = TestLifecycleOwner()
                val viewModel = createViewModel()

                viewModel.registerForActivityResult(
                    activityResultCaller = activityResultCaller,
                    lifecycleOwner = lifecycleOwner,
                )

                assertThat(awaitRegisterCall().contract).isEqualTo(AutocompleteContract)

                val autocompleteLauncher = awaitNextRegisteredLauncher()

                val confirmationRegisterCall = registerTurbine.awaitItem()

                assertThat(confirmationRegisterCall.activityResultCaller).isEqualTo(activityResultCaller)
                assertThat(confirmationRegisterCall.lifecycleOwner).isEqualTo(lifecycleOwner)

                lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

                assertThat(awaitNextUnregisteredLauncher()).isEqualTo(autocompleteLauncher)
            }
        }

    private fun testConfirmationStateRestorationAfterPaymentSuccess(
        loadStateBeforePaymentResult: Boolean
    ) = confirmationTest(
        hasReloadedFromProcessDeath = true,
        emitNullResults = false,
    ) {
        val stripeIntent = PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)

        val paymentSheetLoader = RelayingPaymentElementLoader()

        val viewModel = createViewModel(
            stripeIntent = stripeIntent,
            paymentElementLoader = paymentSheetLoader,
        )

        fun loadState() {
            paymentSheetLoader.enqueueSuccess(
                stripeIntent = stripeIntent,
                validationError = PaymentIntentInTerminalState(StripeIntent.Status.Succeeded),
            )
        }

        fun emitPaymentResult() {
            awaitResultTurbine.add(
                ConfirmationHandler.Result.Succeeded(
                    intent = stripeIntent,
                    deferredIntentConfirmationType = null,
                )
            )
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

            assertThat(awaitItem()).isEqualTo(PaymentSheetResult.Completed())
        }
    }

    private fun selectionSavedTest(
        initializationMode: InitializationMode = ARGS_CUSTOMER_WITH_GOOGLEPAY.initializationMode,
        customerRequestedSave: PaymentSelection.CustomerRequestedSave =
            PaymentSelection.CustomerRequestedSave.NoRequest,
        shouldSave: Boolean = true
    ) = confirmationTest {
        val intent = PAYMENT_INTENT_WITH_PAYMENT_METHOD!!

        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
                initializationMode = initializationMode
            ),
            stripeIntent = PAYMENT_INTENT,
        )

        val createParams = PaymentMethodCreateParams.create(
            card = PaymentMethodCreateParams.Card()
        )
        val selection = PaymentSelection.New.Card(
            brand = CardBrand.Visa,
            customerRequestedSave = customerRequestedSave,
            paymentMethodCreateParams = createParams
        )

        viewModel.updateSelection(selection)
        viewModel.checkout()

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
                intent = intent,
                deferredIntentConfirmationType = null,
            )
        )

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

    private fun FakeConfirmationHandler.Scenario.createViewModel(
        args: PaymentSheetContract.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        stripeIntent: StripeIntent = PAYMENT_INTENT,
        customer: CustomerState? = EMPTY_CUSTOMER_STATE.copy(paymentMethods = PAYMENT_METHODS),
        linkConfigurationCoordinator: LinkConfigurationCoordinator =
            this@PaymentSheetViewModelTest.linkConfigurationCoordinator,
        customerRepository: CustomerRepository =
            FakeCustomerRepository(customer?.paymentMethods ?: emptyList()),
        shouldFailLoad: Boolean = false,
        linkState: LinkState? = null,
        isGooglePayReady: Boolean = false,
        delay: Duration = Duration.ZERO,
        initialPaymentSelection: PaymentSelection? =
            customer?.paymentMethods?.firstOrNull()?.let { PaymentSelection.Saved(it) },
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
        eventReporter: EventReporter = this@PaymentSheetViewModelTest.eventReporter,
        cvcRecollectionHandler: CvcRecollectionHandler = this@PaymentSheetViewModelTest.cvcRecollectionHandler,
        cvcRecollectionInteractor: FakeCvcRecollectionInteractor = FakeCvcRecollectionInteractor(),
    ): PaymentSheetViewModel {
        return createViewModel(
            args = args,
            stripeIntent = stripeIntent,
            customer = customer,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            customerRepository = customerRepository,
            shouldFailLoad = shouldFailLoad,
            linkState = linkState,
            isGooglePayReady = isGooglePayReady,
            delay = delay,
            initialPaymentSelection = initialPaymentSelection,
            validationError = validationError,
            savedStateHandle = savedStateHandle,
            paymentElementLoader = paymentElementLoader,
            errorReporter = errorReporter,
            eventReporter = eventReporter,
            cvcRecollectionHandler = cvcRecollectionHandler,
            cvcRecollectionInteractor = cvcRecollectionInteractor,
            confirmationHandlerFactory = { handler }
        )
    }

    private fun createViewModel(
        args: PaymentSheetContract.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        stripeIntent: StripeIntent = PAYMENT_INTENT,
        customer: CustomerState? = EMPTY_CUSTOMER_STATE.copy(paymentMethods = PAYMENT_METHODS),
        linkConfigurationCoordinator: LinkConfigurationCoordinator = this.linkConfigurationCoordinator,
        customerRepository: CustomerRepository = FakeCustomerRepository(customer?.paymentMethods ?: emptyList()),
        shouldFailLoad: Boolean = false,
        linkState: LinkState? = null,
        isGooglePayReady: Boolean = false,
        delay: Duration = Duration.ZERO,
        initialPaymentSelection: PaymentSelection? =
            customer?.paymentMethods?.firstOrNull()?.let { PaymentSelection.Saved(it) },
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
        confirmationHandlerFactory: ConfirmationHandler.Factory? = null
    ): PaymentSheetViewModel {
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
                confirmationHandlerFactory = confirmationHandlerFactory ?: ConfirmationHandler.Factory {
                    FakeConfirmationHandler().apply {
                        awaitResultTurbine.add(null)
                        awaitResultTurbine.add(null)
                    }
                },
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
                },
                isLiveModeProvider = { false }
            )
        }
    }

    private fun FakeConfirmationHandler.Scenario.createLinkViewModel(): PaymentSheetViewModel {
        val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
            attachNewCardToAccountResult = Result.success(LinkTestUtils.LINK_SAVED_PAYMENT_DETAILS),
            accountStatus = AccountStatus.Verified,
        )

        return createViewModel(
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            confirmationHandlerFactory = {
                handler
            },
            linkState = LinkState(
                configuration = LinkTestUtils.createLinkConfiguration(),
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            )
        )
    }

    private fun FakeConfirmationHandler.Scenario.createViewModelForDeferredIntent(
        args: PaymentSheetContract.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        paymentIntent: PaymentIntent = PAYMENT_INTENT,
    ): PaymentSheetViewModel {
        return createViewModelForDeferredIntent(
            args = args,
            paymentIntent = paymentIntent,
            confirmationHandlerFactory = { handler },
        )
    }

    private fun createViewModelForDeferredIntent(
        args: PaymentSheetContract.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        confirmationHandlerFactory: ConfirmationHandler.Factory? = null,
        paymentIntent: PaymentIntent = PAYMENT_INTENT,
    ): PaymentSheetViewModel {
        val deferredIntent = paymentIntent.copy(id = null, clientSecret = null)

        val intentConfig = IntentConfiguration(
            mode = IntentConfiguration.Mode.Payment(amount = 12345, currency = "usd"),
        )

        return createViewModel(
            args = args.copy(initializationMode = InitializationMode.DeferredIntent(intentConfig)),
            confirmationHandlerFactory = confirmationHandlerFactory,
            stripeIntent = deferredIntent,
        )
    }

    private suspend fun testButtonTypeParsedToProperGooglePayButtonType(
        buttonType: GooglePayConfiguration.ButtonType,
        googlePayButtonType: GooglePayButtonType
    ) {
        val viewModel = createViewModel(
            isGooglePayReady = true,
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
                config = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.config.newBuilder()
                    .googlePay(
                        ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.googlePayConfig?.prefillCreate(
                            buttonType = buttonType,
                        )
                    )
                    .build()
            )
        )

        viewModel.walletsState.test {
            assertThat(awaitItem()?.googlePay?.buttonType).isEqualTo(googlePayButtonType)
        }
    }

    private fun createLinkInlinePaymentSelection(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
        input: UserInput,
    ): PaymentSelection.New.LinkInline {
        return PaymentSelection.New.LinkInline(
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            brand = CardBrand.Visa,
            customerRequestedSave = customerRequestedSave,
            input = input,
        )
    }

    private fun createBacsPaymentSelection(): PaymentSelection.New.GenericPaymentMethod {
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

    private fun confirmationTest(
        hasReloadedFromProcessDeath: Boolean = false,
        emitNullResults: Boolean = true,
        block: suspend FakeConfirmationHandler.Scenario.(scope: TestScope) -> Unit,
    ) = runTest {
        FakeConfirmationHandler.test(
            hasReloadedFromProcessDeath = hasReloadedFromProcessDeath,
            initialState = ConfirmationHandler.State.Idle,
        ) {
            if (emitNullResults) {
                awaitResultTurbine.add(null)
                awaitResultTurbine.add(null)
            }

            block(this@runTest)
        }
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
