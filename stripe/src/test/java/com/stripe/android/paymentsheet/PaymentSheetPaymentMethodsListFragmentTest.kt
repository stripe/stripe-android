package com.stripe.android.paymentsheet

import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PaymentSheetFragmentFactory
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSheetPaymentMethodsListFragmentTest {
    private val eventReporter = mock<EventReporter>()

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
        val savedPaymentMethod = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        val scenario = createScenario()
        scenario.onFragment {
            assertThat(activityViewModel(it).selection.value).isNull()
            fragmentViewModel(it).currentPaymentSelection = savedPaymentMethod
        }
        scenario.recreate()
        scenario.onFragment {
            assertThat(activityViewModel(it).selection.value)
                .isEqualTo(savedPaymentMethod)
        }
    }

    @Test
    fun `sets up adapter`() {
        createScenario().onFragment {
            val recycler = recyclerView(it)
            val adapter = recycler.adapter as PaymentOptionsAdapter
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
        val savedPaymentMethod = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        createScenario().onFragment {
            val activityViewModel = activityViewModel(it)
            activityViewModel.setPaymentMethods(paymentMethods)
            idleLooper()

            val recycler = recyclerView(it)
            assertThat(recycler.adapter).isInstanceOf(PaymentOptionsAdapter::class.java)
            val adapter = recycler.adapter as PaymentOptionsAdapter
            adapter.paymentMethodSelectedListener(savedPaymentMethod)
            idleLooper()

            assertThat(fragmentViewModel(it).currentPaymentSelection)
                .isEqualTo(savedPaymentMethod)
            assertThat(activityViewModel.selection.value)
                .isEqualTo(savedPaymentMethod)
        }
    }

    @Test
    fun `posts transition when add card clicked`() {
        createScenario().onFragment {
            val activityViewModel = activityViewModel(it)
            assertThat(activityViewModel.transition.value).isNull()

            activityViewModel.setPaymentMethods(paymentMethods)
            idleLooper()

            val recycler = recyclerView(it)
            assertThat(recycler.adapter).isInstanceOf(PaymentOptionsAdapter::class.java)
            val adapter = recycler.adapter as PaymentOptionsAdapter
            adapter.addCardClickListener.onClick(it.requireView())
            idleLooper()

            assertThat(activityViewModel.transition.value)
                .isEqualTo(PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull)
        }
    }

    @Test
    fun `click on GooglePay item should update selection`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(fragment)
            activityViewModel.setPaymentMethods(paymentMethods)
            idleLooper()

            val recycler = recyclerView(fragment)
            val adapter = recycler.adapter as PaymentOptionsAdapter
            adapter.shouldShowGooglePay = true
            idleLooper()

            val googlePayView = recycler.children.toList()[1]
            googlePayView.performClick()

            assertThat(activityViewModel.selection.value)
                .isEqualTo(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `updateHeader() should update header view`() {
        createScenario().onFragment { fragment ->
            assertThat(fragment.header.text.toString())
                .isEqualTo("Pay using")
            fragment.updateHeader(amount = 1099, currencyCode = "usd")
            assertThat(fragment.header.text.toString())
                .isEqualTo("Pay $10.99 using")
        }
    }

    @Test
    fun `started fragment should report onShowExistingPaymentOptions() event`() {
        createScenario().onFragment {
            verify(eventReporter).onShowExistingPaymentOptions()
        }
    }

    private fun recyclerView(it: PaymentSheetPaymentMethodsListFragment) =
        it.requireView().findViewById<RecyclerView>(R.id.recycler)

    private fun activityViewModel(
        fragment: PaymentSheetPaymentMethodsListFragment
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory(
                { fragment.requireActivity().application },
                { PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY }
            )
        }.value
    }

    private fun fragmentViewModel(
        fragment: PaymentSheetPaymentMethodsListFragment
    ) = fragment.viewModels<BasePaymentMethodsListFragment.PaymentMethodsViewModel>().value

    private fun createScenario(): FragmentScenario<PaymentSheetPaymentMethodsListFragment> {
        return launchFragmentInContainer<PaymentSheetPaymentMethodsListFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_STARTER_ARGS to PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
            ),
            R.style.StripePaymentSheetDefaultTheme,
            factory = PaymentSheetFragmentFactory(eventReporter)
        )
    }
}
