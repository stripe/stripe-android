package com.stripe.android.checkout

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.AbsFakeStripeRepository
import com.stripe.android.ApiRequest
import com.stripe.android.StripeRepository
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.ActivityStarter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CheckoutViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val activity =
        Activity().apply {
            intent = Intent().putExtra(
                ActivityStarter.Args.EXTRA,
                CheckoutActivityStarter.Args(
                    "client_secret",
                    "ephemeral_key",
                    "customer_id"
                )
            )
        }
    private val stripeRepository: StripeRepository = FakeStripeRepository()
    private val testDispatcher = TestCoroutineDispatcher()
    private val viewModel = CheckoutViewModel(
        ApplicationProvider.getApplicationContext(),
        "publishable_key",
        "stripe_account_id",
        stripeRepository,
        testDispatcher
    )

    @Test
    fun `getPaymentMethods should fetch from api repository`() = testDispatcher.runBlockingTest {
        var paymentMethods: List<PaymentMethod>? = null
        viewModel.getPaymentMethods(activity).observeForever {
            paymentMethods = it
        }
        assertThat(paymentMethods).containsExactly(FAKE_CARD)
    }

    @Test
    fun `getPaymentMethods should call onError when no args supplied`() {
        var error: Throwable? = null
        viewModel.error.observeForever {
            error = it
        }
        viewModel.getPaymentMethods(Activity().apply { intent = Intent() })
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `getPaymentMethods should not update multipe times`() = testDispatcher.runBlockingTest {
        var updates = 0
        viewModel.getPaymentMethods(activity).observeForever {
            updates += 1
        }
        viewModel.getPaymentMethods(activity).observeForever {}
        assertThat(updates).isEqualTo(1)
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override fun getPaymentMethods(listPaymentMethodsParams: ListPaymentMethodsParams, publishableKey: String, productUsageTokens: Set<String>, requestOptions: ApiRequest.Options): List<PaymentMethod> {
            return listOf(FAKE_CARD)
        }
    }

    private companion object {
        val FAKE_CARD = PaymentMethod("fake_pm", created = 0, liveMode = false, type = PaymentMethod.Type.Card)
    }
}
