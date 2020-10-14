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
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest

@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val intent = Intent().putExtra(
        ActivityStarter.Args.EXTRA,
        PaymentSheetActivityStarter.Args(
            "client_secret",
            "ephemeral_key",
            "customer_id"
        )
    )
    private val stripeRepository: StripeRepository = FakeStripeRepository()
    private val paymentController: PaymentController = mock()
    private val viewModel = PaymentSheetViewModel(
        "publishable_key",
        "stripe_account_id",
        stripeRepository,
        paymentController
    )

    private val activity: Activity = mock()
    private val callbackCaptor: KArgumentCaptor<ApiResultCallback<PaymentIntentResult>> = argumentCaptor()

    @BeforeTest
    fun before() {
        whenever(activity.intent).thenReturn(intent)
        whenever(paymentController.shouldHandlePaymentResult(any(), any())).thenReturn(true)
    }

    @Test
    fun `updatePaymentMethods should fetch from api repository`() {
        var paymentMethods: List<PaymentMethod>? = null
        viewModel.paymentMethods.observeForever {
            paymentMethods = it
        }
        viewModel.updatePaymentMethods(intent)
        assertThat(paymentMethods).containsExactly(FAKE_CARD)
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
    fun `onActivityResult should update paymentIntentResult`() {
        val paymentIntentResult: PaymentIntentResult = mock()
        whenever(paymentController.handlePaymentResult(any(), callbackCaptor.capture())).doAnswer {
            callbackCaptor.lastValue.onSuccess(paymentIntentResult)
        }

        viewModel.onActivityResult(0, 0, intent)
        assertThat(viewModel.paymentIntentResult.value).isSameInstanceAs(paymentIntentResult)
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
        viewModel.onActivityResult(0, 0, intent)
        assertThat(error).isNotNull()
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
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
    }
}
