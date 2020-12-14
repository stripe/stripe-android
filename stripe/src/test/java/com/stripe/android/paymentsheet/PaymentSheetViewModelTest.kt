package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.googlepay.StripeGooglePayLauncher
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
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private val paymentIntent = PaymentIntentFixtures.PI_WITH_SHIPPING

    private val googlePayRepository = FakeGooglePayRepository(true)
    private val stripeRepository: StripeRepository = FakeStripeRepository(paymentIntent)
    private val paymentController: PaymentController = mock()
    private val prefsRepository = mock<PrefsRepository>()
    private val eventReporter = mock<EventReporter>()
    private val viewModel: PaymentSheetViewModel by lazy { createViewModel() }

    private val callbackCaptor = argumentCaptor<ApiResultCallback<PaymentIntentResult>>()

    @BeforeTest
    fun before() {
        whenever(paymentController.shouldHandlePaymentResult(any(), any()))
            .thenReturn(true)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
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
            stripeRepository = FailingStripeRepository()
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
    fun `checkout() should not attempt to confirm when no payment selection has been mode`() {
        verifyNoInteractions(paymentController)
        viewModel.checkout(mock())
        verify(prefsRepository).savePaymentSelection(null)
    }

    @Test
    fun `checkout() should confirm saved payment methods`() {
        val paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout(mock())

        verify(prefsRepository).savePaymentSelection(paymentSelection)
        verify(paymentController).startConfirmAndAuth(
            any(),
            eq(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    requireNotNull(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id),
                    CLIENT_SECRET
                )
            ),
            eq(
                ApiRequest.Options(
                    "publishable_key",
                    "stripe_account_id",
                )
            )
        )
    }

    @Test
    fun `checkout() should confirm new payment methods`() {
        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            shouldSavePaymentMethod = true
        )
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout(mock())

        verify(prefsRepository).savePaymentSelection(paymentSelection)
        verify(paymentController).startConfirmAndAuth(
            any(),
            eq(
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CLIENT_SECRET,
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            ),
            eq(
                ApiRequest.Options(
                    "publishable_key",
                    "stripe_account_id",
                )
            )
        )
    }

    @Test
    fun `onActivityResult() should update ViewState LiveData`() {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(selection)

        whenever(paymentController.handlePaymentResult(any(), callbackCaptor.capture())).doAnswer {
            callbackCaptor.lastValue.onSuccess(PAYMENT_INTENT_RESULT)
        }

        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }

        viewModel.onActivityResult(0, 0, Intent())
        assertThat(viewState)
            .isEqualTo(
                ViewState.Completed(PAYMENT_INTENT_RESULT)
            )

        verify(eventReporter)
            .onPaymentSuccess(selection)
    }

    @Test
    fun `onActivityResult() with non-success outcome should report failure event`() {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        viewModel.updateSelection(selection)

        whenever(paymentController.handlePaymentResult(any(), callbackCaptor.capture())).doAnswer {
            callbackCaptor.lastValue.onSuccess(
                PAYMENT_INTENT_RESULT.copy(
                    outcomeFromFlow = StripeIntentResult.Outcome.FAILED
                )
            )
        }

        viewModel.onActivityResult(0, 0, Intent())
        verify(eventReporter)
            .onPaymentFailure(selection)
    }

    @Test
    fun `onActivityResult() should update emit API errors`() {
        var userMessage: SheetViewModel.UserMessage? = null
        viewModel.userMessage.observeForever {
            userMessage = it
        }
        whenever(paymentController.handlePaymentResult(any(), callbackCaptor.capture())).doAnswer {
            callbackCaptor.lastValue.onError(
                RuntimeException("Your card was declined.")
            )
        }
        viewModel.onActivityResult(0, 0, Intent())
        assertThat(userMessage)
            .isEqualTo(
                SheetViewModel.UserMessage.Error("Your card was declined.")
            )
    }

    @Test
    fun `onActivityResult with successful Google Pay result should emit on googlePayCompletion`() {
        val paymentIntentResults = mutableListOf<PaymentIntentResult>()
        viewModel.googlePayCompletion.observeForever { paymentIntentResult ->
            if (paymentIntentResult != null) {
                paymentIntentResults.add(paymentIntentResult)
            }
        }
        viewModel.onActivityResult(
            StripeGooglePayLauncher.REQUEST_CODE,
            Activity.RESULT_OK,
            Intent().putExtras(
                StripeGooglePayLauncher.Result.PaymentIntent(
                    PAYMENT_INTENT_RESULT
                ).toBundle()
            )
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
            stripeRepository = FailingStripeRepository()
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
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrievePaymentIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): PaymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                )
            }
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
        Dispatchers.setMain(testCoroutineDispatcher)
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
    fun `fetchAddPaymentMethodConfig() when all data is ready should emit value`() {
        viewModel.fetchPaymentIntent()
        viewModel.fetchIsGooglePayReady()
        viewModel.updatePaymentMethods()

        val configs = mutableListOf<AddPaymentMethodConfig>()
        viewModel.fetchAddPaymentMethodConfig().observeForever { config ->
            if (config != null) {
                configs.add(config)
            }
        }
        viewModel.fetchAddPaymentMethodConfig()

        assertThat(configs)
            .hasSize(1)
    }

    private fun createViewModel(
        args: PaymentSheetActivityStarter.Args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
        stripeRepository: StripeRepository = this.stripeRepository
    ): PaymentSheetViewModel {
        return PaymentSheetViewModel(
            "publishable_key",
            "stripe_account_id",
            stripeRepository,
            paymentController,
            googlePayRepository,
            prefsRepository,
            eventReporter,
            args,
            workContext = testCoroutineDispatcher
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

    private class FakeStripeRepository(
        var paymentIntent: PaymentIntent
    ) : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent {
            return paymentIntent
        }

        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> {
            return listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }
    }

    private companion object {
        private const val CLIENT_SECRET = PaymentSheetFixtures.CLIENT_SECRET
        private val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
        private val ARGS_WITHOUT_CUSTOMER = PaymentSheetFixtures.ARGS_WITHOUT_CUSTOMER

        val PAYMENT_INTENT_RESULT = PaymentIntentResult(
            intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
        )
    }
}
