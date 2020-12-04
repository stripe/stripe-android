package com.stripe.android.paymentsheet

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
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.viewmodels.SheetViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
    private val viewModel = PaymentSheetViewModel(
        "publishable_key",
        "stripe_account_id",
        stripeRepository,
        paymentController,
        googlePayRepository,
        prefsRepository,
        DEFAULT_ARGS,
        workContext = testCoroutineDispatcher
    )

    private val callbackCaptor = argumentCaptor<ApiResultCallback<PaymentIntentResult>>()

    @BeforeTest
    fun before() {
        whenever(paymentController.shouldHandlePaymentResult(any(), any()))
            .thenReturn(true)
    }

    @Test
    fun `updatePaymentMethods() with default args should fetch from API repository`() {
        var paymentMethods: List<PaymentMethod>? = null
        viewModel.paymentMethods.observeForever {
            paymentMethods = it
        }
        viewModel.updatePaymentMethods()
        assertThat(paymentMethods)
            .containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    @Test
    fun `updatePaymentMethods() with guest args should emit empty list`() {
        val guestModeViewModel = PaymentSheetViewModel(
            "publishable_key",
            "stripe_account_id",
            stripeRepository,
            paymentController,
            googlePayRepository,
            prefsRepository,
            GUEST_ARGS,
            workContext = testCoroutineDispatcher
        )
        var paymentMethods: List<PaymentMethod>? = null
        guestModeViewModel.paymentMethods.observeForever {
            paymentMethods = it
        }
        guestModeViewModel.updatePaymentMethods()
        assertThat(paymentMethods)
            .isEmpty()
    }

    @Test
    fun `checkout() should call onError() when no payment selection has been mode`() {
        var error: Throwable? = null
        viewModel.fatal.observeForever {
            error = it
        }

        viewModel.checkout(mock())

        verify(prefsRepository).savePaymentSelection(null)
        assertThat(error)
            .isInstanceOf(IllegalStateException::class.java)
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
        val paymentSelection = PaymentSelection.New(PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
        viewModel.updateSelection(paymentSelection)
        viewModel.checkout(mock())

        verify(prefsRepository).savePaymentSelection(paymentSelection)
        verify(paymentController).startConfirmAndAuth(
            any(),
            eq(
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CLIENT_SECRET,
                    setupFutureUsage = null
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
    fun `checkout() should call onError when no payment method selected`() {
        var error: Throwable? = null
        viewModel.fatal.observeForever {
            error = it
        }
        viewModel.checkout(mock())
        assertThat(error)
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `onActivityResult() should update ViewState LiveData`() {
        val paymentIntentResult = PaymentIntentResult(
            intent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            outcomeFromFlow = StripeIntentResult.Outcome.SUCCEEDED
        )
        whenever(paymentController.handlePaymentResult(any(), callbackCaptor.capture())).doAnswer {
            callbackCaptor.lastValue.onSuccess(paymentIntentResult)
        }

        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }

        viewModel.onActivityResult(0, 0, Intent())
        assertThat(viewState)
            .isEqualTo(
                ViewState.Completed(paymentIntentResult)
            )
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
        val exception = RuntimeException("It failed")
        val viewModel = PaymentSheetViewModel(
            "publishable_key",
            "stripe_account_id",
            object : AbsFakeStripeRepository() {
                override suspend fun retrievePaymentIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): PaymentIntent? {
                    throw exception
                }
            },
            paymentController,
            googlePayRepository,
            prefsRepository,
            DEFAULT_ARGS,
            workContext = testCoroutineDispatcher
        )
        var error: Throwable? = null
        viewModel.fatal.observeForever {
            error = it
        }
        viewModel.fetchPaymentIntent()
        assertThat(error)
            .isEqualTo(exception)
    }

    @Test
    fun `fetchPaymentIntent() should fail if confirmationMethod=manual`() {
        val viewModel = PaymentSheetViewModel(
            "publishable_key",
            "stripe_account_id",
            object : AbsFakeStripeRepository() {
                override suspend fun retrievePaymentIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): PaymentIntent = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                )
            },
            paymentController,
            googlePayRepository,
            prefsRepository,
            DEFAULT_ARGS,
            workContext = testCoroutineDispatcher
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
    fun `isGooglePayReady should emit expected value`() {
        var isReady: Boolean? = null
        viewModel.isGooglePayReady.observeForever {
            isReady = it
        }
        viewModel.fetchIsGooglePayReady()
        assertThat(isReady)
            .isTrue()
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
        private val DEFAULT_ARGS = PaymentSheetFixtures.DEFAULT_ARGS
        private val GUEST_ARGS = PaymentSheetFixtures.GUEST_ARGS
    }
}
