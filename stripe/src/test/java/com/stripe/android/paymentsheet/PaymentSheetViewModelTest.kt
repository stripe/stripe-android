package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.AbsFakeStripeRepository
import com.stripe.android.ApiRequest
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeRepository
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private val paymentIntent = PaymentIntentFixtures.PI_WITH_SHIPPING

    private val stripeRepository: StripeRepository = FakeStripeRepository(paymentIntent)
    private val paymentController: PaymentController = mock()
    private val viewModel = PaymentSheetViewModel(
        "publishable_key",
        "stripe_account_id",
        stripeRepository,
        paymentController,
        workContext = testCoroutineDispatcher
    )

    private val activity: Activity = mock()
    private val callbackCaptor: KArgumentCaptor<ApiResultCallback<PaymentIntentResult>> = argumentCaptor()

    @BeforeTest
    fun before() {
        whenever(activity.intent).thenReturn(DEFAULT_ARGS_INTENT)
        whenever(paymentController.shouldHandlePaymentResult(any(), any())).thenReturn(true)
    }

    @Test
    fun `updatePaymentMethods with default args should fetch from API repository`() {
        var paymentMethods: List<PaymentMethod>? = null
        viewModel.paymentMethods.observeForever {
            paymentMethods = it
        }
        viewModel.updatePaymentMethods(DEFAULT_ARGS_INTENT)
        assertThat(paymentMethods).containsExactly(FAKE_CARD)
    }

    @Test
    fun `updatePaymentMethods with guest args should not fetch from API repository`() {
        var count = 0
        viewModel.paymentMethods.observeForever {
            count++
        }
        viewModel.updatePaymentMethods(GUEST_ARGS_INTENT)
        assertThat(count)
            .isEqualTo(0)
    }

    @Test
    fun `updatePaymentMethods should call onError when no args supplied`() {
        var error: Throwable? = null
        viewModel.error.observeForever {
            error = it
        }
        viewModel.updatePaymentMethods(Intent())
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `checkout should call onError when no args supplied`() {
        var error: Throwable? = null
        viewModel.error.observeForever {
            error = it
        }
        val activity: Activity = mock()
        whenever(activity.intent).thenReturn(Intent())
        viewModel.checkout(activity)
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `checkout should confirm saved payment methods`() {
        viewModel.updateSelection(PaymentSelection.Saved("saved_pm"))
        viewModel.checkout(activity)
        verify(paymentController).startConfirmAndAuth(
            any(),
            eq(
                ConfirmPaymentIntentParams.createWithPaymentMethodId(
                    "saved_pm",
                    "client_secret"
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
    fun `checkout should confirm new payment methods`() {
        val createParams: PaymentMethodCreateParams = mock()
        viewModel.updateSelection(PaymentSelection.New(createParams))
        viewModel.checkout(activity)
        verify(paymentController).startConfirmAndAuth(
            any(),
            eq(
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                    createParams,
                    "client_secret"
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
    fun `checkout should call onError when no payment method selected`() {
        var error: Throwable? = null
        viewModel.error.observeForever {
            error = it
        }
        viewModel.checkout(activity)
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `onActivityResult should update ViewState LiveData`() {
        val paymentIntentResult: PaymentIntentResult = mock()
        whenever(paymentController.handlePaymentResult(any(), callbackCaptor.capture())).doAnswer {
            callbackCaptor.lastValue.onSuccess(paymentIntentResult)
        }

        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }

        viewModel.onActivityResult(0, 0, DEFAULT_ARGS_INTENT)
        assertThat(viewState)
            .isEqualTo(
                ViewState.Completed(paymentIntentResult)
            )
    }

    @Test
    fun `onActivityResult should update propagate errors`() {
        var error: Throwable? = null
        viewModel.error.observeForever {
            error = it
        }
        whenever(paymentController.handlePaymentResult(any(), callbackCaptor.capture())).doAnswer {
            callbackCaptor.lastValue.onError(RuntimeException("some exception"))
        }
        viewModel.onActivityResult(0, 0, DEFAULT_ARGS_INTENT)
        assertThat(error).isNotNull()
    }

    @Test
    fun `fetchPaymentIntent() should update ViewState LiveData`() {
        var viewState: ViewState? = null
        viewModel.viewState.observeForever {
            viewState = it
        }
        viewModel.fetchPaymentIntent(DEFAULT_ARGS_INTENT)
        assertThat(viewState)
            .isEqualTo(
                ViewState.Ready(amount = 1099, currencyCode = "usd")
            )
    }

    @Test
    fun `fetchPaymentIntent should propagate errors`() {
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
            workContext = testCoroutineDispatcher
        )
        var error: Throwable? = null
        viewModel.error.observeForever {
            error = it
        }
        viewModel.fetchPaymentIntent(DEFAULT_ARGS_INTENT)
        assertThat(error).isEqualTo(exception)
    }

    @Test
    fun `fetchPaymentIntent should call onError when no args supplied`() {
        var error: Throwable? = null
        viewModel.error.observeForever {
            error = it
        }
        viewModel.fetchPaymentIntent(Intent())
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    private class FakeStripeRepository(val paymentIntent: PaymentIntent) : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent? {
            return paymentIntent
        }

        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> {
            return listOf(FAKE_CARD)
        }
    }

    private companion object {
        val FAKE_CARD = PaymentMethod("fake_pm", created = 0, liveMode = false, type = PaymentMethod.Type.Card)

        private val DEFAULT_ARGS = PaymentSheetActivityStarter.Args.Default(
            "client_secret",
            "ephemeral_key",
            "customer_id"
        )

        private val GUEST_ARGS = PaymentSheetActivityStarter.Args.Guest(
            "client_secret"
        )

        private val DEFAULT_ARGS_INTENT = Intent()
            .putExtra(
                ActivityStarter.Args.EXTRA,
                DEFAULT_ARGS
            )

        private val GUEST_ARGS_INTENT = Intent()
            .putExtra(
                ActivityStarter.Args.EXTRA,
                GUEST_ARGS
            )
    }
}
