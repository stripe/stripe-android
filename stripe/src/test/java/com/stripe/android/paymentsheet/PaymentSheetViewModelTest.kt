package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.FakePaymentFlowResultProcessor
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.repositories.PaymentIntentRepository
import com.stripe.android.paymentsheet.repositories.PaymentMethodsRepository
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    private val googlePayRepository = FakeGooglePayRepository(true)
    private val prefsRepository = FakePrefsRepository()
    private val eventReporter = mock<EventReporter>()
    private val viewModel: PaymentSheetViewModel by lazy { createViewModel() }
    private val paymentFlowResultProcessor = FakePaymentFlowResultProcessor()

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
        viewModel.updatePaymentMethods()
        assertThat(paymentMethods)
            .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    @Test
    fun `updatePaymentMethods() with customer config and failing request should emit empty list`() {
        val viewModel = createViewModel(
            paymentMethodsRepository = PaymentMethodsRepository.Api(
                stripeRepository = FailingStripeRepository(),
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccountId = null,
                workContext = testDispatcher
            )
        )
        var paymentMethods: List<PaymentMethod>? = null
        viewModel.paymentMethods.observeForever {
            paymentMethods = it
        }
        viewModel.updatePaymentMethods()
        assertThat(requireNotNull(paymentMethods))
            .isEmpty()
    }

    @Test
    fun `updatePaymentMethods() without customer config should emit empty list`() {
        val viewModelWithoutCustomer = createViewModel(ARGS_WITHOUT_CUSTOMER)
        var paymentMethods: List<PaymentMethod>? = null
        viewModelWithoutCustomer.paymentMethods.observeForever {
            paymentMethods = it
        }
        viewModelWithoutCustomer.updatePaymentMethods()
        assertThat(paymentMethods)
            .isEmpty()
    }

    @Test
    fun `checkout() should not attempt to confirm when no payment selection has been mode`() = testDispatcher.runBlockingTest {
        viewModel.checkout()
        assertThat(prefsRepository.paymentSelectionArgs)
            .containsExactly(null)
        assertThat(prefsRepository.getSavedSelection())
            .isEqualTo(SavedSelection.None)
    }

    @Test
    fun `checkout() should confirm saved payment methods`() = testDispatcher.runBlockingTest {
        val confirmParams = mutableListOf<ConfirmPaymentIntentParams>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        assertThat(prefsRepository.paymentSelectionArgs)
            .containsExactly(paymentSelection)
        assertThat(prefsRepository.getSavedSelection())
            .isEqualTo(
                SavedSelection.PaymentMethod(paymentSelection.paymentMethod.id.orEmpty())
            )

        assertThat(confirmParams)
            .containsExactly(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    requireNotNull(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id),
                    CLIENT_SECRET
                )
            )
    }

    @Test
    fun `checkout() should confirm new payment methods`() = testDispatcher.runBlockingTest {
        val confirmParams = mutableListOf<ConfirmPaymentIntentParams>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            shouldSavePaymentMethod = true
        )
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout()

        assertThat(confirmParams)
            .containsExactly(
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CLIENT_SECRET,
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )

        assertThat(prefsRepository.paymentSelectionArgs)
            .containsExactly(paymentSelection)
        assertThat(prefsRepository.getSavedSelection())
            .isEqualTo(SavedSelection.None)
    }

    @Test
    fun `onPaymentFlowResult() should update ViewState LiveData`() {
        paymentFlowResultProcessor.paymentIntentResult = PAYMENT_INTENT_RESULT

        val confirmParams = mutableListOf<ConfirmPaymentIntentParams>()
        viewModel.startConfirm.observeForever {
            confirmParams.add(it)
        }

        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(selection)

        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }

        viewModel.onPaymentFlowResult(
            PaymentFlowResult.Unvalidated(
                "client_secret",
                StripeIntentResult.Outcome.SUCCEEDED
            )
        )
        assertThat(viewState)
            .isEqualTo(
                ViewState.Completed(PAYMENT_INTENT_RESULT)
            )

        verify(eventReporter)
            .onPaymentSuccess(selection)
    }

    @Test
    fun `onPaymentFlowResult() with non-success outcome should report failure event`() {
        paymentFlowResultProcessor.paymentIntentResult = PAYMENT_INTENT_RESULT.copy(
            outcomeFromFlow = StripeIntentResult.Outcome.FAILED
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
    fun `onPaymentFlowResult() should update emit API errors`() {
        paymentFlowResultProcessor.error = RuntimeException("Your card was declined.")

        var userMessage: SheetViewModel.UserMessage? = null
        viewModel.userMessage.observeForever {
            userMessage = it
        }
        viewModel.onPaymentFlowResult(
            PaymentFlowResult.Unvalidated()
        )
        assertThat(userMessage)
            .isEqualTo(
                SheetViewModel.UserMessage.Error("Your card was declined.")
            )
    }

    @Test
    fun `onGooglePayResult() with successful Google Pay result should emit on googlePayCompletion`() {
        val paymentIntentResults = mutableListOf<PaymentIntentResult>()
        viewModel.googlePayCompletion.observeForever { paymentIntentResult ->
            if (paymentIntentResult != null) {
                paymentIntentResults.add(paymentIntentResult)
            }
        }
        viewModel.onGooglePayResult(
            StripeGooglePayContract.Result.PaymentIntent(PAYMENT_INTENT_RESULT)
        )

        assertThat(paymentIntentResults)
            .containsExactly(PAYMENT_INTENT_RESULT)

        verify(eventReporter)
            .onPaymentSuccess(PaymentSelection.GooglePay)
    }

    @Test
    fun `fetchPaymentIntent() should update ViewState LiveData`() {
        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.fetchPaymentIntent()
        assertThat(viewState)
            .isEqualTo(
                ViewState.Ready(amount = 1099, currencyCode = "usd")
            )
    }

    @Test
    fun `fetchPaymentIntent() should propagate errors`() {
        val viewModel = createViewModel(
            paymentIntentRepository = PaymentIntentRepository.Api(
                stripeRepository = FailingStripeRepository(),
                requestOptions = ApiRequest.Options(
                    apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
                ),
                workContext = testDispatcher
            )
        )
        var error: Throwable? = null
        viewModel.fatal.observeForever {
            error = it
        }
        viewModel.fetchPaymentIntent()
        assertThat(error?.message)
            .isEqualTo("Could not parse PaymentIntent.")
    }

    @Test
    fun `fetchPaymentIntent() should fail if confirmationMethod=manual`() {
        val viewModel = createViewModel(
            paymentIntentRepository = PaymentIntentRepository.Static(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                )
            )
        )
        var error: Throwable? = null
        viewModel.fatal.observeForever {
            error = it
        }
        viewModel.fetchPaymentIntent()
        assertThat(error?.message)
            .isEqualTo(
                "PaymentIntent with confirmation_method='automatic' is required.\n" +
                    "See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method."
            )
    }

    @Test
    fun `fetchPaymentIntent() should fail if status != requires_payment_method`() {
        val viewModel = createViewModel(
            paymentIntentRepository = PaymentIntentRepository.Static(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2
            )
        )
        var error: Throwable? = null
        viewModel.fatal.observeForever {
            error = it
        }
        viewModel.fetchPaymentIntent()
        assertThat(error?.message)
            .isEqualTo(
                "PaymentIntent with confirmation_method='automatic' is required.\n" +
                    "See https://stripe.com/docs/api/payment_intents/object#payment_intent_object-confirmation_method."
            )
    }

    @Test
    fun `isGooglePayReady when googlePayConfig is not null should emit expected value`() {
        Dispatchers.setMain(testDispatcher)
        var isReady: Boolean? = null
        viewModel.isGooglePayReady.observeForever {
            isReady = it
        }
        assertThat(isReady)
            .isTrue()
    }

    @Test
    fun `isGooglePayReady without google pay config should emit false`() {
        val viewModel = createViewModel(PaymentSheetFixtures.ARGS_CUSTOMER_WITHOUT_GOOGLEPAY)
        var isReady: Boolean? = null
        viewModel.isGooglePayReady.observeForever {
            isReady = it
        }
        viewModel.fetchIsGooglePayReady()
        assertThat(isReady)
            .isFalse()
    }

    @Test
    fun `fetchFragmentConfig() when all data is ready should emit value`() {
        viewModel.fetchPaymentIntent()
        viewModel.fetchIsGooglePayReady()
        viewModel.updatePaymentMethods()

        val configs = mutableListOf<FragmentConfig>()
        viewModel.fetchFragmentConfig().observeForever { config ->
            if (config != null) {
                configs.add(config)
            }
        }

        assertThat(configs)
            .hasSize(1)
    }

    @Test
    fun `buyButton is only enabled when not processing, transition target, and a selection has been made`() {
        var isEnabled = false
        viewModel.ctaEnabled.observeForever {
            isEnabled = it
        }

        assertThat(isEnabled)
            .isFalse()

        viewModel.transitionTo(
            PaymentSheetViewModel.TransitionTarget.SelectSavedPaymentMethod(
                FragmentConfigFixtures.DEFAULT
            )
        )
        assertThat(isEnabled)
            .isFalse()

        viewModel.updateSelection(PaymentSelection.GooglePay)
        assertThat(isEnabled)
            .isFalse()

        viewModel.fetchPaymentIntent()
        assertThat(isEnabled)
            .isTrue()
    }

    private fun createViewModel(
        args: PaymentSheetContract.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        paymentIntentRepository: PaymentIntentRepository = PaymentIntentRepository.Static(PAYMENT_INTENT),
        paymentMethodsRepository: PaymentMethodsRepository = PaymentMethodsRepository.Static(PAYMENT_METHODS)
    ): PaymentSheetViewModel {
        return PaymentSheetViewModel(
            "publishable_key",
            "stripe_account_id",
            paymentIntentRepository = paymentIntentRepository,
            paymentMethodsRepository = paymentMethodsRepository,
            paymentFlowResultProcessor,
            googlePayRepository,
            prefsRepository,
            eventReporter,
            args,
            animateOutMillis = 0,
            workContext = testDispatcher
        )
    }

    private class FailingStripeRepository : AbsFakeStripeRepository() {
        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> = error("Request failed.")
    }

    private companion object {
        private const val CLIENT_SECRET = PaymentSheetFixtures.CLIENT_SECRET
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        private val ARGS_WITHOUT_CUSTOMER = PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER

        private val PAYMENT_METHODS = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val PAYMENT_INTENT_RESULT = PaymentIntentResult(
            intent = PAYMENT_INTENT,
            outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
        )
    }
}
