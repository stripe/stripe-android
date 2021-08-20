package com.stripe.android.paymentsheet

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.Status
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.paymentsheet.PaymentSheetViewModel.CheckoutIdentifier
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSheetViewState
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.UserErrorMessage
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.lang.IllegalStateException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private val prefsRepository = FakePrefsRepository()
    private val eventReporter = mock<EventReporter>()
    private val viewModel: PaymentSheetViewModel by lazy { createViewModel() }
    private val paymentFlowResultProcessor = mock<PaymentIntentFlowResultProcessor>()
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Captor
    private lateinit var paymentMethodTypeCaptor: ArgumentCaptor<List<PaymentMethod.Type>>

    @BeforeTest
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `init should fire analytics event`() {
        createViewModel()
        verify(eventReporter)
            .onInit(PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY)
    }

    @Test
    fun `updatePaymentMethods() with customer config should fetch from API repository`() {
        var paymentMethods: List<PaymentMethod>? = null
        viewModel.paymentMethods.observeForever {
            paymentMethods = it
        }
        viewModel.updatePaymentMethods(PAYMENT_INTENT)
        assertThat(paymentMethods)
            .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    @Test
    fun `updatePaymentMethods() with customer config and failing request should emit empty list`() =
        runBlockingTest {
            val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
            val failingStripeRepository: StripeRepository = mock()
            whenever(
                failingStripeRepository.getPaymentMethods(
                    any(),
                    anyString(),
                    anySet(),
                    any()
                )
            ).doThrow(IllegalStateException("Request Failed"))

            val viewModel = createViewModel(
                customerRepository = CustomerApiRepository(
                    stripeRepository = failingStripeRepository,
                    lazyPaymentConfig = { paymentConfiguration },
                    logger = Logger.getInstance(false),
                    workContext = testDispatcher
                )
            )
            var paymentMethods: List<PaymentMethod>? = null
            viewModel.paymentMethods.observeForever {
                paymentMethods = it
            }
            viewModel.updatePaymentMethods(PAYMENT_INTENT)
            idleLooper()
            assertThat(requireNotNull(paymentMethods))
                .isEmpty()
        }

    @Test
    fun `removePaymentMethod triggers async removal`() = runBlockingTest {
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
    fun `updatePaymentMethods() without customer config should emit empty list`() {
        val viewModelWithoutCustomer = createViewModel(ARGS_WITHOUT_CUSTOMER)
        var paymentMethods: List<PaymentMethod>? = null
        viewModelWithoutCustomer.paymentMethods.observeForever {
            paymentMethods = it
        }
        viewModelWithoutCustomer.updatePaymentMethods(PAYMENT_INTENT)
        assertThat(paymentMethods)
            .isEmpty()
    }

    @Test
    fun `updatePaymentMethods() should fetch only supported payment method types`() =
        testDispatcher.runBlockingTest {
            val paymentMethodsRepository = mock<CustomerRepository>()
            val viewModel = createViewModel(
                customerRepository = paymentMethodsRepository
            )
            val stripeIntent = PAYMENT_INTENT.copy(
                paymentMethodTypes = listOf(
                    "card", // valid and supported
                    "fpx", // valid but not supported
                    "invalid_type" // unknown type
                )
            )

            viewModel.updatePaymentMethods(stripeIntent)
            verify(paymentMethodsRepository).getPaymentMethods(
                any(),
                capture(paymentMethodTypeCaptor)
            )
            assertThat(paymentMethodTypeCaptor.value)
                .containsExactly(PaymentMethod.Type.Card)
        }

    @Test
    fun `updatePaymentMethods() should filter out invalid payment method types`() {
        val viewModel = createViewModel(
            customerRepository = FakeCustomerRepository(
                listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(card = null), // invalid
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )
        )

        var paymentMethods: List<PaymentMethod>? = null
        viewModel.paymentMethods.observeForever {
            paymentMethods = it
        }

        viewModel.updatePaymentMethods(PAYMENT_INTENT)
        assertThat(paymentMethods)
            .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    @Test
    fun `checkout() should confirm saved payment methods`() = testDispatcher.runBlockingTest {
        val confirmParams = mutableListOf<BaseSheetViewModel.Event<ConfirmStripeIntentParams>>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout(CheckoutIdentifier.None)

        assertThat(confirmParams).hasSize(1)
        assertThat(confirmParams[0].peekContent())
            .isEqualTo(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    requireNotNull(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id),
                    CLIENT_SECRET
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
            PaymentSelection.Saved(PaymentMethodFixtures.AU_BECS_DEBIT_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout(CheckoutIdentifier.None)

        assertThat(events).hasSize(1)
        val confirmParams = events[0].peekContent() as ConfirmSetupIntentParams
        assertThat(confirmParams.mandateData)
            .isNotNull()
    }

    @Test
    fun `checkout() should confirm new payment methods`() = testDispatcher.runBlockingTest {
        val confirmParams = mutableListOf<BaseSheetViewModel.Event<ConfirmStripeIntentParams>>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            shouldSavePaymentMethod = true
        )
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout(CheckoutIdentifier.None)

        assertThat(confirmParams).hasSize(1)
        assertThat(confirmParams[0].peekContent())
            .isEqualTo(
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CLIENT_SECRET,
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
    }

    @Test
    fun `Google Pay checkout cancelled returns to Ready state`() {
        viewModel.fetchStripeIntent()
        viewModel.updateSelection(PaymentSelection.GooglePay)
        viewModel.checkout(CheckoutIdentifier.AddFragmentTopGooglePay)

        val viewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.getButtonStateObservable(CheckoutIdentifier.AddFragmentTopGooglePay)
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

        assertThat(viewState.size).isEqualTo(2)
        assertThat(processing.size).isEqualTo(2)
        assertThat(viewState[1])
            .isEqualTo(PaymentSheetViewState.Reset(null))
        assertThat(processing[1]).isFalse()
    }

    @Test
    fun `On checkout clear the previous view state error`() {

        val googleViewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.checkoutIdentifier = CheckoutIdentifier.AddFragmentTopGooglePay
        viewModel.getButtonStateObservable(CheckoutIdentifier.AddFragmentTopGooglePay)
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

        viewModel.checkout(CheckoutIdentifier.SheetBottomBuy)

        assertThat(googleViewState[0]).isNull()
        assertThat(googleViewState[1]).isEqualTo(PaymentSheetViewState.Reset(null))
        assertThat(buyViewState[0]).isEqualTo(PaymentSheetViewState.StartProcessing)
    }

    @Test
    fun `Google Pay checkout failed returns to Ready state and shows error`() {
        viewModel.fetchStripeIntent()
        viewModel.updateSelection(PaymentSelection.GooglePay)
        viewModel.checkout(CheckoutIdentifier.AddFragmentTopGooglePay)

        val viewState: MutableList<PaymentSheetViewState?> = mutableListOf()
        viewModel.getButtonStateObservable(CheckoutIdentifier.AddFragmentTopGooglePay)
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

        assertThat(processing.size).isEqualTo(2)

        assertThat(viewState.size).isEqualTo(2)
        assertThat(viewState[1])
            .isEqualTo(PaymentSheetViewState.Reset(UserErrorMessage("An internal error occurred.")))
        assertThat(processing[1]).isFalse()
    }

    @Test
    fun `onPaymentFlowResult() should update ViewState and save preferences`() =
        testDispatcher.runBlockingTest {
            whenever(paymentFlowResultProcessor.processResult(any())).thenReturn(
                PAYMENT_INTENT_RESULT
            )

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

            viewModel.onPaymentFlowResult(
                PaymentFlowResult.Unvalidated(
                    "client_secret",
                    StripeIntentResult.Outcome.SUCCEEDED
                )
            )
            assertThat(viewState[1])
                .isInstanceOf(PaymentSheetViewState.FinishProcessing::class.java)

            (viewState[1] as PaymentSheetViewState.FinishProcessing).onComplete()

            assertThat(paymentSheetResult).isEqualTo(PaymentSheetResult.Completed)

            verify(eventReporter)
                .onPaymentSuccess(selection)

            assertThat(prefsRepository.paymentSelectionArgs)
                .containsExactly(selection)
            assertThat(prefsRepository.getSavedSelection(true))
                .isEqualTo(
                    SavedSelection.PaymentMethod(selection.paymentMethod.id.orEmpty())
                )
        }

    @Test
    fun `onPaymentFlowResult() should update ViewState and save new payment method`() =
        testDispatcher.runBlockingTest {
            whenever(paymentFlowResultProcessor.processResult(any())).thenReturn(
                PAYMENT_INTENT_RESULT_WITH_PM
            )

            val selection = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                CardBrand.Visa,
                shouldSavePaymentMethod = true
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

            viewModel.onPaymentFlowResult(
                PaymentFlowResult.Unvalidated(
                    "client_secret",
                    StripeIntentResult.Outcome.SUCCEEDED
                )
            )
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
            assertThat(prefsRepository.getSavedSelection(true))
                .isEqualTo(
                    SavedSelection.PaymentMethod(
                        PAYMENT_INTENT_RESULT_WITH_PM.intent.paymentMethod!!.id!!
                    )
                )
        }

    @Test
    fun `onPaymentFlowResult() with non-success outcome should report failure event`() =
        testDispatcher.runBlockingTest {
            whenever(paymentFlowResultProcessor.processResult(any())).thenReturn(
                PAYMENT_INTENT_RESULT.copy(
                    outcomeFromFlow = StripeIntentResult.Outcome.FAILED
                )
            )

            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            viewModel.updateSelection(selection)

            var stripeIntent: StripeIntent? = null
            viewModel.stripeIntent.observeForever {
                stripeIntent = it
            }

            viewModel.onPaymentFlowResult(
                PaymentFlowResult.Unvalidated()
            )
            verify(eventReporter)
                .onPaymentFailure(selection)

            assertThat(stripeIntent).isNull()
        }

    @Test
    fun `onPaymentFlowResult() with processing status for payment method which has delay should report success event`() =
        testDispatcher.runBlockingTest {
            whenever(paymentFlowResultProcessor.processResult(any())).thenReturn(
                PaymentIntentResult(
                    PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                        paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
                        status = StripeIntent.Status.Processing
                    ),
                    StripeIntentResult.Outcome.UNKNOWN
                )
            )

            val selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD)
            viewModel.updateSelection(selection)

            viewModel.onPaymentFlowResult(
                PaymentFlowResult.Unvalidated()
            )
            verify(eventReporter)
                .onPaymentSuccess(selection)
        }

    @Test
    fun `onPaymentFlowResult() with processing status for payment method which does not have delay should report failure event`() =
        testDispatcher.runBlockingTest {
            whenever(paymentFlowResultProcessor.processResult(any())).thenReturn(
                PaymentIntentResult(
                    PaymentIntentFixtures.PI_WITH_SHIPPING.copy(
                        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        status = StripeIntent.Status.Processing
                    ),
                    StripeIntentResult.Outcome.UNKNOWN
                )
            )

            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            viewModel.updateSelection(selection)

            viewModel.onPaymentFlowResult(
                PaymentFlowResult.Unvalidated()
            )
            verify(eventReporter)
                .onPaymentFailure(selection)
        }

    @Test
    fun `onPaymentFlowResult() should update emit API errors`() =
        testDispatcher.runBlockingTest {
            whenever(paymentFlowResultProcessor.processResult(any())).thenThrow(
                RuntimeException("Your card was declined.")
            )

            viewModel.fetchStripeIntent()

            val viewStateList = mutableListOf<PaymentSheetViewState>()
            viewModel.viewState.observeForever {
                viewStateList.add(it)
            }
            viewModel.onPaymentFlowResult(
                PaymentFlowResult.Unvalidated()
            )

            assertThat(viewStateList[0])
                .isEqualTo(
                    PaymentSheetViewState.Reset(null)
                )
            assertThat(viewStateList[1])
                .isEqualTo(
                    PaymentSheetViewState.Reset(
                        UserErrorMessage("Your card was declined.")
                    )
                )
        }

    @Test
    fun `fetchPaymentIntent() should update ViewState LiveData`() {
        var viewState: PaymentSheetViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.fetchStripeIntent()
        assertThat(viewState)
            .isEqualTo(
                PaymentSheetViewState.Reset(null)
            )
    }

    @Test
    fun `fetchPaymentIntent() should propagate errors`() = runBlocking {
        val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        val failingStripeRepository: StripeRepository = mock()
        whenever(
            failingStripeRepository.getPaymentMethods(
                any(),
                anyString(),
                anySet(),
                any()
            )
        ).doThrow(IllegalStateException("Request Failed"))

        val viewModel = createViewModel(
            stripeIntentRepository = StripeIntentRepository.Api(
                stripeRepository = failingStripeRepository,
                lazyPaymentConfig = { paymentConfiguration },
                workContext = testDispatcher
            )
        )
        var result: PaymentSheetResult? = null
        viewModel.paymentSheetResult.observeForever {
            result = it
        }
        viewModel.fetchStripeIntent()
        assertThat((result as? PaymentSheetResult.Failed)?.error?.message)
            .isEqualTo("Could not parse PaymentIntent.")
    }

    @Test
    fun `fetchPaymentIntent() should fail if confirmationMethod=manual`() {
        val viewModel = createViewModel(
            stripeIntentRepository = StripeIntentRepository.Static(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                )
            )
        )
        var result: PaymentSheetResult? = null
        viewModel.paymentSheetResult.observeForever {
            result = it
        }
        viewModel.fetchStripeIntent()
        assertThat((result as? PaymentSheetResult.Failed)?.error?.message)
            .isEqualTo(
                "PaymentIntent with confirmation_method='automatic' is required.\n" +
                    "The current PaymentIntent has confirmation_method Manual.\n" +
                    "See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method."
            )
    }

    @Test
    fun `fetchPaymentIntent() should fail if status != requires_payment_method`() {
        val viewModel = createViewModel(
            stripeIntentRepository = StripeIntentRepository.Static(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            )
        )
        var result: PaymentSheetResult? = null
        viewModel.paymentSheetResult.observeForever {
            result = it
        }
        viewModel.fetchStripeIntent()
        assertThat((result as? PaymentSheetResult.Failed)?.error?.message)
            .isEqualTo(
                "PaymentIntent with confirmation_method='automatic' is required.\n" +
                    "The current PaymentIntent has confirmation_method Manual.\n" +
                    "See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method."
            )
    }

    @Test
    fun `when StripeIntent does not accept any of the supported payment methods should return error`() {
        val viewModel = createViewModel(
            stripeIntentRepository = StripeIntentRepository.Static(
                PAYMENT_INTENT.copy(paymentMethodTypes = listOf("unsupported_payment_type"))
            )
        )
        var result: PaymentSheetResult? = null
        viewModel.paymentSheetResult.observeForever {
            result = it
        }
        viewModel.fetchStripeIntent()
        assertThat((result as? PaymentSheetResult.Failed)?.error?.message)
            .startsWith(
                "None of the requested payment methods ([unsupported_payment_type]) " +
                    "match the supported payment types "
            )
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
                    googlePay = com.stripe.android.paymentsheet.ConfigFixtures.GOOGLE_PAY.copy(
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
    fun `fragmentConfig when all data is ready should emit value`() {
        viewModel.fetchStripeIntent()
        viewModel._isGooglePayReady.value = true

        val configs = mutableListOf<FragmentConfig>()
        viewModel.fragmentConfig.observeForever { config ->
            if (config != null) {
                configs.add(config)
            }
        }

        assertThat(configs)
            .hasSize(1)
    }

    @Test
    fun `buyButton is only enabled when not processing, not editing, and a selection has been made`() {
        var isEnabled = false
        viewModel.ctaEnabled.observeForever {
            isEnabled = it
        }

        assertThat(isEnabled)
            .isFalse()

        viewModel.updateSelection(PaymentSelection.GooglePay)
        assertThat(isEnabled)
            .isFalse()

        viewModel.fetchStripeIntent()
        assertThat(isEnabled)
            .isTrue()

        viewModel.setEditing(true)
        assertThat(isEnabled)
            .isFalse()
    }

    @Test
    fun `Should show amount is true for PaymentIntent`() {
        viewModel.fetchStripeIntent()

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
    fun `viewState should emit FinishProcessing and ProcessResult if PaymentIntent is confirmed`() {
        val viewModel = createViewModel(
            stripeIntentRepository = StripeIntentRepository.Static(
                PaymentIntentFixtures.PI_SUCCEEDED
            )
        )

        val viewStates = mutableListOf<PaymentSheetViewState>()
        viewModel.viewState.observeForever { viewState ->
            if (viewState is PaymentSheetViewState.FinishProcessing) {
                // force `onComplete` to be called
                viewState.onComplete()
            }
            viewState?.let {
                viewStates.add(it)
            }
        }

        var paymentSheetResult: PaymentSheetResult? = null
        viewModel.paymentSheetResult.observeForever {
            paymentSheetResult = it
        }

        viewModel.fetchStripeIntent()

        assertThat(viewStates)
            .hasSize(1)
        assertThat(viewStates[0])
            .isInstanceOf(PaymentSheetViewState.FinishProcessing::class.java)
        assertThat(paymentSheetResult).isEqualTo(PaymentSheetResult.Completed)
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

    private fun createViewModel(
        args: PaymentSheetContract.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        stripeIntentRepository: StripeIntentRepository = StripeIntentRepository.Static(
            PAYMENT_INTENT
        ),
        customerRepository: CustomerRepository = FakeCustomerRepository(
            PAYMENT_METHODS
        )
    ): PaymentSheetViewModel {
        val paymentConfiguration = PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        return PaymentSheetViewModel(
            application,
            args,
            eventReporter,
            { paymentConfiguration },
            stripeIntentRepository,
            customerRepository,
            { paymentFlowResultProcessor },
            prefsRepository,
            Logger.noop(),
            testDispatcher,
            mock(),
            mock()
        )
    }

    private companion object {
        private const val CLIENT_SECRET = PaymentSheetFixtures.CLIENT_SECRET
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP =
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        private val ARGS_WITHOUT_CUSTOMER = PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER

        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val PAYMENT_INTENT_RESULT = PaymentIntentResult(
            intent = PAYMENT_INTENT,
            outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
        )

        val PAYMENT_INTENT_WITH_PM = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val PAYMENT_INTENT_RESULT_WITH_PM = PaymentIntentResult(
            intent = PAYMENT_INTENT_WITH_PM,
            outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
        )
    }
}
