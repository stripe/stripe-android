package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.AbsFakeStripeRepository
import com.stripe.android.ApiRequest
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.StripeRepository
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.RuntimeException
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
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
    private val testDispatcher = TestCoroutineDispatcher()
    private val stripe: Stripe = mock()
    private val viewModel = PaymentSheetViewModel(
        ApplicationProvider.getApplicationContext(),
        "publishable_key",
        "stripe_account_id",
        stripeRepository,
        stripe,
        testDispatcher
    )

    private val activity: Activity = mock()

    @BeforeTest
    fun before() {
        whenever(activity.intent).thenReturn(intent)
    }

    @Test
    fun `updatePaymentMethods should fetch from api repository`() = testDispatcher.runBlockingTest {
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
        verify(stripe).confirmPayment(
            activity,
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                "saved_pm",
                "client_secret"
            )
        )
    }

    @Test
    fun `checkout should confirm new payment methods`() {
        val createParams: PaymentMethodCreateParams = mock()
        viewModel.updateSelection(PaymentSelection.New(createParams))
        viewModel.checkout(activity)
        verify(stripe).confirmPayment(
            activity,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                createParams,
                "client_secret"
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
        whenever(stripe.onPaymentResult(any(), any(), any())).doAnswer {
            val callback = it.arguments[2] as ApiResultCallback<PaymentIntentResult>
            callback.onSuccess(paymentIntentResult)
            true
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
        whenever(stripe.onPaymentResult(any(), any(), any())).doAnswer {
            val callback = it.arguments[2] as ApiResultCallback<PaymentIntentResult>
            callback.onError(RuntimeException("some exception"))
            true
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
