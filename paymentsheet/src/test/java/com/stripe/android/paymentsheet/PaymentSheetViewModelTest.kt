package com.stripe.android.paymentsheet

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.android.gms.common.api.Status
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.TransitionTarget
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.UserErrorMessage
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakePaymentSheetLoader
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
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

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val eventReporter = mock<EventReporter>()
    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val lpmRepository = LpmRepository(
        arguments = LpmRepository.LpmRepositoryArguments(application.resources),
    ).apply {
        this.forceUpdate(
            listOf(
                PaymentMethod.Type.Card.code,
                PaymentMethod.Type.USBankAccount.code,
                PaymentMethod.Type.Ideal.code,
                PaymentMethod.Type.SepaDebit.code,
                PaymentMethod.Type.Sofort.code,
                PaymentMethod.Type.Affirm.code,
                PaymentMethod.Type.AfterpayClearpay.code,
            ),
            null
        )
    }

    private val prefsRepository = FakePrefsRepository()
    private val lpmResourceRepository = StaticLpmResourceRepository(lpmRepository)

    private val linkLauncher = mock<LinkPaymentLauncher> {
        on { getAccountStatusFlow(any()) } doReturn flowOf(AccountStatus.SignedOut)
    }

    private val primaryButtonUIState = PrimaryButton.UIState(
        label = "Test",
        onClick = {},
        enabled = true,
        visible = true
    )

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
        createViewModel()
        verify(eventReporter).onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `removePaymentMethod triggers async removal`() = runTest {
        val customerRepository = spy(FakeCustomerRepository())
        val viewModel = createViewModel(
            customerRepository = customerRepository
        )

        viewModel.removePaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        idleLooper()

        verify(customerRepository).detachPaymentMethod(
            any(),
            eq(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!)
        )
    }

    @Test
    fun `checkout() should confirm saved card payment methods`() = runTest {
        val viewModel = createViewModel()
        val confirmParams = mutableListOf<BaseSheetViewModel.Event<ConfirmStripeIntentParams>>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        assertThat(confirmParams).hasSize(1)
        assertThat(confirmParams[0].peekContent())
            .isEqualTo(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    requireNotNull(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id),
                    CLIENT_SECRET,
                    paymentMethodOptions = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                    )
                )
            )
    }

    @Test
    fun `checkout() should confirm saved us_bank_account payment methods`() = runTest {
        val viewModel = createViewModel()
        val confirmParams = mutableListOf<BaseSheetViewModel.Event<ConfirmStripeIntentParams>>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        assertThat(confirmParams).hasSize(1)
        assertThat(confirmParams[0].peekContent())
            .isEqualTo(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    requireNotNull(PaymentMethodFixtures.US_BANK_ACCOUNT.id),
                    CLIENT_SECRET,
                    paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    ),
                    mandateData = MandateDataParams(
                        type = MandateDataParams.Type.Online.DEFAULT
                    )
                )
            )
    }

    @Test
    fun `checkout() for Setup Intent with saved payment method that requires mandate should include mandate`() {
        val viewModel = createViewModel(
            args = ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        )

        val events = mutableListOf<BaseSheetViewModel.Event<ConfirmStripeIntentParams>>()
        viewModel.startConfirm.observeForever {
            events.add(it)
        }

        val paymentSelection =
            PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        assertThat(events).hasSize(1)
        val confirmParams = events[0].peekContent() as ConfirmSetupIntentParams
        assertThat(confirmParams.mandateData)
            .isNotNull()
    }

    @Test
    fun `checkout() should confirm new payment methods`() = runTest {
        val viewModel = createViewModel()
        val confirmParams = mutableListOf<BaseSheetViewModel.Event<ConfirmStripeIntentParams>>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        assertThat(confirmParams).hasSize(1)
        assertThat(confirmParams[0].peekContent())
            .isEqualTo(
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CLIENT_SECRET,
                    paymentMethodOptions = PaymentMethodOptionsParams.Card(
                        setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                    )
                )
            )
    }

    @Test
    fun `checkout() with shipping should confirm new payment methods`() = runTest {
        val confirmParams = mutableListOf<BaseSheetViewModel.Event<ConfirmStripeIntentParams>>()

        val viewModel = createViewModel(
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

        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        assertThat(confirmParams).hasSize(1)
        assertThat(confirmParams[0].peekContent())
            .isEqualTo(
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CLIENT_SECRET,
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
            )
    }

    @Test
    fun `Launches Link when user is logged in to their Link account`() = runTest {
        val configuration: LinkPaymentLauncher.Configuration = mock()

        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = configuration,
                loginState = LinkState.LoginState.LoggedIn,
            ),
        )

        assertThat(viewModel.showLinkVerificationDialog.value).isFalse()
        assertThat(viewModel.activeLinkSession.value).isTrue()
        assertThat(viewModel.isLinkEnabled.value).isTrue()

        verify(linkLauncher).present(
            configuration = eq(configuration),
            prefilledNewCardParams = isNull(),
        )
    }

    @Test
    fun `Launches Link verification when user needs to verify their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.NeedsVerification,
            ),
        )

        assertThat(viewModel.showLinkVerificationDialog.value).isTrue()
        assertThat(viewModel.activeLinkSession.value).isFalse()
        assertThat(viewModel.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Enables Link when user is logged out of their Link account`() = runTest {
        val viewModel = createViewModel(
            linkState = LinkState(
                configuration = mock(),
                loginState = LinkState.LoginState.LoggedOut,
            ),
        )

        assertThat(viewModel.activeLinkSession.value).isFalse()
        assertThat(viewModel.isLinkEnabled.value).isTrue()
    }

    @Test
    fun `Does not enable Link when the Link state can't be determined`() = runTest {
        val viewModel = createViewModel(
            linkState = null,
        )

        assertThat(viewModel.activeLinkSession.value).isFalse()
        assertThat(viewModel.isLinkEnabled.value).isFalse()
    }

    @Test
    fun `Google Pay checkout cancelled returns to Ready state`() {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        val viewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetTopGooglePay)
            .observeForever {
                viewState.add(it)
            }

        val processing: MutableList<Boolean> = mutableListOf()
        viewModel.processing.observeForever {
            processing.add(it)
        }

        assertThat(viewState.size).isEqualTo(1)
        assertThat(processing.size).isEqualTo(1)
        assertThat(viewState[0])
            .isEqualTo(PaymentSheetViewState.StartProcessing)
        assertThat(processing[0]).isTrue()

        viewModel.onGooglePayResult(GooglePayPaymentMethodLauncher.Result.Canceled)
        assertThat(viewModel.contentVisible.value).isTrue()

        assertThat(viewState.size).isEqualTo(2)
        assertThat(processing.size).isEqualTo(2)
        assertThat(viewState[1])
            .isEqualTo(PaymentSheetViewState.Reset(null))
        assertThat(processing[1]).isFalse()
    }

    @Test
    fun `On checkout clear the previous view state error`() {
        val viewModel = createViewModel()
        val googleViewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.checkoutIdentifier = CheckoutIdentifier.SheetTopGooglePay
        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetTopGooglePay)
            .observeForever {
                googleViewState.add(it)
            }

        val buyViewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetBottomBuy)
            .observeForever {
                buyViewState.add(it)
            }

        val viewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.viewState.observeForever {
            viewState.add(it)
        }

        viewModel.checkout()

        assertThat(googleViewState[0]).isEqualTo(PaymentSheetViewState.Reset(null))
        assertThat(buyViewState[0]).isEqualTo(PaymentSheetViewState.StartProcessing)
    }

    @Test
    fun `Google Pay checkout failed returns to Ready state and shows error`() {
        val viewModel = createViewModel()

        viewModel.checkoutWithGooglePay()

        val viewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.getButtonStateObservable(CheckoutIdentifier.SheetTopGooglePay)
            .observeForever {
                viewState.add(it)
            }

        val processing: MutableList<Boolean> = mutableListOf()
        viewModel.processing.observeForever {
            processing.add(it)
        }

        assertThat(viewState.size).isEqualTo(1)
        assertThat(processing.size).isEqualTo(1)
        assertThat(viewState[0]).isEqualTo(PaymentSheetViewState.StartProcessing)
        assertThat(processing[0]).isTrue()

        viewModel.onGooglePayResult(
            GooglePayPaymentMethodLauncher.Result.Failed(
                Exception("Test exception"),
                Status.RESULT_INTERNAL_ERROR.statusCode
            )
        )

        assertThat(viewModel.contentVisible.value).isTrue()
        assertThat(processing.size).isEqualTo(2)
        assertThat(viewState.size).isEqualTo(2)
        assertThat(viewState[1])
            .isEqualTo(PaymentSheetViewState.Reset(UserErrorMessage("An internal error occurred.")))
        assertThat(processing[1]).isFalse()
    }

    @Test
    fun `onPaymentResult() should update ViewState and save preferences`() =
        runTest {
            val viewModel = createViewModel()

            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            viewModel.updateSelection(selection)

            val viewState: MutableList<PaymentSheetViewState?> = mutableListOf()
            viewModel.viewState.observeForever {
                viewState.add(it)
            }

            var paymentSheetResult: PaymentSheetResult? = null
            viewModel.paymentSheetResult.observeForever {
                paymentSheetResult = it
            }

            viewModel.onPaymentResult(PaymentResult.Completed)

            assertThat(viewState[1])
                .isInstanceOf(PaymentSheetViewState.FinishProcessing::class.java)

            (viewState[1] as PaymentSheetViewState.FinishProcessing).onComplete()

            assertThat(paymentSheetResult).isEqualTo(PaymentSheetResult.Completed)

            verify(eventReporter)
                .onPaymentSuccess(selection)

            assertThat(prefsRepository.paymentSelectionArgs)
                .containsExactly(selection)
            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(
                    SavedSelection.PaymentMethod(selection.paymentMethod.id.orEmpty())
                )
        }

    @Test
    fun `onPaymentResult() should update ViewState and save new payment method`() =
        runTest {
            val viewModel = createViewModel(stripeIntent = PAYMENT_INTENT_WITH_PM)

            val selection = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
            viewModel.updateSelection(selection)

            val viewState: MutableList<PaymentSheetViewState?> = mutableListOf()
            viewModel.viewState.observeForever {
                viewState.add(it)
            }

            var paymentSheetResult: PaymentSheetResult? = null
            viewModel.paymentSheetResult.observeForever {
                paymentSheetResult = it
            }

            viewModel.onPaymentResult(PaymentResult.Completed)

            assertThat(viewState[1])
                .isInstanceOf(PaymentSheetViewState.FinishProcessing::class.java)

            (viewState[1] as PaymentSheetViewState.FinishProcessing).onComplete()

            assertThat(paymentSheetResult).isEqualTo(PaymentSheetResult.Completed)

            verify(eventReporter)
                .onPaymentSuccess(selection)

            assertThat(prefsRepository.paymentSelectionArgs)
                .containsExactly(
                    PaymentSelection.Saved(
                        PAYMENT_INTENT_RESULT_WITH_PM.intent.paymentMethod!!
                    )
                )
            assertThat(prefsRepository.getSavedSelection(true, true))
                .isEqualTo(
                    SavedSelection.PaymentMethod(
                        PAYMENT_INTENT_RESULT_WITH_PM.intent.paymentMethod!!.id!!
                    )
                )
        }

    @Test
    fun `onPaymentResult() with non-success outcome should report failure event`() = runTest {
        val viewModel = createViewModel(shouldFailLoad = true)
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(selection)

        viewModel.stripeIntent.test {
            viewModel.onPaymentResult(PaymentResult.Failed(Throwable()))
            verify(eventReporter).onPaymentFailure(selection)

            val stripeIntent = awaitItem()
            assertThat(stripeIntent).isNull()
        }
    }

    @Test
    fun `onPaymentResult() should update emit API errors`() =
        runTest {
            val viewModel = createViewModel()

            val viewStateList = mutableListOf<PaymentSheetViewState>()
            viewModel.viewState.observeForever {
                viewStateList.add(it)
            }

            val errorMessage = "very helpful error message"
            viewModel.onPaymentResult(PaymentResult.Failed(Throwable(errorMessage)))

            assertThat(viewStateList[0])
                .isEqualTo(
                    PaymentSheetViewState.Reset(null)
                )
            assertThat(viewStateList[1])
                .isEqualTo(
                    PaymentSheetViewState.Reset(
                        UserErrorMessage(errorMessage)
                    )
                )
        }

    @Test
    fun `fetchPaymentIntent() should update ViewState LiveData`() {
        val viewModel = createViewModel()
        var viewState: PaymentSheetViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }

        assertThat(viewState)
            .isEqualTo(
                PaymentSheetViewState.Reset(null)
            )
    }

    @Test
    fun `Loading payment sheet state should propagate errors`() = runBlocking {
        val viewModel = createViewModel(shouldFailLoad = true)
        var result: PaymentSheetResult? = null
        viewModel.paymentSheetResult.observeForever {
            result = it
        }
        assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
    }

    @Test
    fun `when StripeIntent does not accept any of the supported payment methods should return error`() {
        val viewModel = createViewModel(
            stripeIntent = PAYMENT_INTENT.copy(
                paymentMethodTypes = listOf("unsupported_payment_type"),
            ),
        )

        var result: PaymentSheetResult? = null
        viewModel.paymentSheetResult.observeForever {
            result = it
        }

        assertThat((result as? PaymentSheetResult.Failed)?.error?.message)
            .startsWith(
                "None of the requested payment methods ([unsupported_payment_type]) " +
                    "match the supported payment types "
            )
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
    fun `isGooglePayReady without google pay config should emit false`() {
        val viewModel = createViewModel(PaymentSheetFixtures.ARGS_CUSTOMER_WITHOUT_GOOGLEPAY)
        var isReady: Boolean? = null
        viewModel.isGooglePayReady.observeForever {
            isReady = it
        }
        assertThat(isReady)
            .isFalse()
    }

    @Test
    fun `isGooglePayReady for SetupIntent missing currencyCode should emit false`() {
        val viewModel = createViewModel(
            ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP.copy(
                config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.copy(
                    googlePay = ConfigFixtures.GOOGLE_PAY.copy(
                        currencyCode = null
                    )
                )
            )
        )
        var isReady: Boolean? = null
        viewModel.isGooglePayReady.observeForever {
            isReady = it
        }
        assertThat(isReady)
            .isFalse()
    }

    @Test
    fun `googlePayLauncherConfig for SetupIntent with currencyCode should be valid`() {
        val viewModel = createViewModel(ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP)
        assertThat(viewModel.googlePayLauncherConfig)
            .isNotNull()
    }

    @Test
    fun `Transition only happens when view model is ready`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.transitionToFirstScreenWhenReady()
        assertThat(viewModel.backStack.value).isEmpty()

        viewModel._isGooglePayReady.value = true
        assertThat(viewModel.backStack.value).containsExactly(TransitionTarget.AddFirstPaymentMethod)
    }

    @Test
    fun `buyButton is enabled when primaryButtonEnabled is true, else not processing, not editing, and a selection has been made`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        var isEnabled = false

        viewModel.ctaEnabled.observeForever {
            isEnabled = it
        }

        viewModel.savedStateHandle[SAVE_PROCESSING] = false
        viewModel.updateSelection(PaymentSelection.GooglePay)
        viewModel.setEditing(false)

        assertThat(isEnabled).isTrue()

        viewModel.updateSelection(null)
        assertThat(isEnabled).isFalse()

        viewModel.updateSelection(PaymentSelection.GooglePay)
        assertThat(isEnabled).isTrue()

        viewModel.updatePrimaryButtonUIState(primaryButtonUIState.copy(enabled = false))
        assertThat(isEnabled).isFalse()

        viewModel.updatePrimaryButtonUIState(primaryButtonUIState.copy(enabled = true))
        assertThat(isEnabled).isTrue()

        viewModel.savedStateHandle[SAVE_PROCESSING] = true
        assertThat(isEnabled).isFalse()

        viewModel.savedStateHandle[SAVE_PROCESSING] = false
        assertThat(isEnabled).isTrue()

        viewModel.setEditing(true)
        assertThat(isEnabled).isFalse()

        viewModel.setEditing(false)
        assertThat(isEnabled).isTrue()
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
    fun `updateSelection() posts mandate text when selected payment is us_bank_account`() {
        val viewModel = createViewModel()
        viewModel.updateSelection(
            PaymentSelection.Saved(
                PaymentMethodFixtures.US_BANK_ACCOUNT
            )
        )

        assertThat(viewModel.notesText.value)
            .isEqualTo(
                ACHText.getContinueMandateText(ApplicationProvider.getApplicationContext())
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
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
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

    private fun createViewModel(
        args: PaymentSheetContract.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        stripeIntent: StripeIntent = PAYMENT_INTENT,
        customerRepository: CustomerRepository = FakeCustomerRepository(PAYMENT_METHODS),
        shouldFailLoad: Boolean = false,
        linkState: LinkState? = null,
    ): PaymentSheetViewModel {
        val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        return PaymentSheetViewModel(
            application,
            args,
            eventReporter,
            { paymentConfiguration },
            StripeIntentRepository.Static(stripeIntent),
            StripeIntentValidator(),
            FakePaymentSheetLoader(
                stripeIntent = stripeIntent,
                shouldFail = shouldFailLoad,
                linkState = linkState,
            ),
            customerRepository,
            prefsRepository,
            lpmResourceRepository,
            mock(),
            mock(),
            mock(),
            Logger.noop(),
            testDispatcher,
            DUMMY_INJECTOR_KEY,
            savedStateHandle = SavedStateHandle(),
            linkLauncher
        )
    }

    private companion object {
        private const val CLIENT_SECRET = PaymentSheetFixtures.CLIENT_SECRET
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP =
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        private val ARGS_WITHOUT_CUSTOMER = PaymentSheetFixtures.ARGS_WITHOUT_CONFIG

        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val PAYMENT_INTENT_WITH_PM = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val PAYMENT_INTENT_RESULT_WITH_PM = PaymentIntentResult(
            intent = PAYMENT_INTENT_WITH_PM,
            outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
        )
    }
}
