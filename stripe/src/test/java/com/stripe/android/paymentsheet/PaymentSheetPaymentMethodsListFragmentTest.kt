package com.stripe.android.paymentsheet

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSheetPaymentMethodsListFragmentTest {
    private val paymentMethods = listOf(
        PaymentMethod("one", 0, false, PaymentMethod.Type.Card),
        PaymentMethod("two", 0, false, PaymentMethod.Type.Card)
    )

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `resets payment method selection when shown`() {
        val savedPaymentMethod = PaymentSelection.Saved("test_payment_method")

        val scenario = createScenario()
        scenario.onFragment {
            assertThat(activityViewModel(it).selection.value).isNull()
            fragmentViewModel(it).selectedPaymentMethod = savedPaymentMethod
        }
        scenario.recreate()
        scenario.onFragment {
            assertThat(activityViewModel(it).selection.value).isEqualTo(savedPaymentMethod)
        }
    }

    @Test
    fun `sets up adapter`() {
        createScenario().onFragment {
            val recycler = recyclerView(it)
            val adapter = recycler.adapter as PaymentSheetPaymentMethodsAdapter
            assertThat(adapter.paymentMethods)
                .isEmpty()

            activityViewModel(it).setPaymentMethods(paymentMethods)
            idleLooper()

            assertThat(adapter.paymentMethods)
                .isEqualTo(paymentMethods)
        }
    }

    @Test
    fun `updates selection on click`() {
        val savedPaymentMethod = PaymentSelection.Saved("test_payment_method")

        createScenario().onFragment {
            val activityViewModel = activityViewModel(it)
            activityViewModel.setPaymentMethods(paymentMethods)
            idleLooper()

            val recycler = recyclerView(it)
            assertThat(recycler.adapter).isInstanceOf(PaymentSheetPaymentMethodsAdapter::class.java)
            val adapter = recycler.adapter as PaymentSheetPaymentMethodsAdapter
            adapter.paymentMethodSelectedListener(savedPaymentMethod)
            idleLooper()

            assertThat(fragmentViewModel(it).selectedPaymentMethod).isEqualTo(savedPaymentMethod)
            assertThat(activityViewModel.selection.value).isEqualTo(savedPaymentMethod)
        }
    }

    @Test
    fun `posts transition when add card clicked`() {
        val savedPaymentMethod = PaymentSelection.Saved("test_payment_method")

        createScenario().onFragment {
            val activityViewModel = activityViewModel(it)
            assertThat(activityViewModel.transition.value).isNull()

            activityViewModel.setPaymentMethods(paymentMethods)
            idleLooper()

            val recycler = recyclerView(it)
            assertThat(recycler.adapter).isInstanceOf(PaymentSheetPaymentMethodsAdapter::class.java)
            val adapter = recycler.adapter as PaymentSheetPaymentMethodsAdapter
            adapter.addCardClickListener.onClick(it.requireView())
            idleLooper()

            assertThat(activityViewModel.transition.value).isEqualTo(PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull)
        }
    }

    private fun recyclerView(it: PaymentSheetPaymentMethodsListFragment) =
        it.requireView().findViewById<RecyclerView>(R.id.recycler)

    private fun activityViewModel(fragment: PaymentSheetPaymentMethodsListFragment): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory(
                { fragment.requireActivity().application },
                { PaymentSheetFixtures.DEFAULT_ARGS }
            )
        }.value
    }

    private fun fragmentViewModel(fragment: PaymentSheetPaymentMethodsListFragment): PaymentSheetPaymentMethodsListFragment.PaymentMethodsViewModel {
        return fragment.viewModels<PaymentSheetPaymentMethodsListFragment.PaymentMethodsViewModel>().value
    }

    private fun createScenario(): FragmentScenario<PaymentSheetPaymentMethodsListFragment> {
        return launchFragmentInContainer<PaymentSheetPaymentMethodsListFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_STARTER_ARGS to PaymentSheetFixtures.DEFAULT_ARGS
            )
        )
    }
}
