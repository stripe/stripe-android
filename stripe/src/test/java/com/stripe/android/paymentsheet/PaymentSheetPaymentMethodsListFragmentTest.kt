package com.stripe.android.paymentsheet

import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
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
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.ui.PaymentSheetFragmentFactory
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentSheetPaymentMethodsListFragmentTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val eventReporter = mock<EventReporter>()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `resets payment method selection when shown`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(paymentMethod)

        val scenario = createScenario(
            fragmentConfig = FRAGMENT_CONFIG.copy(
                isGooglePayReady = true,
                paymentMethods = listOf(paymentMethod),
                savedSelection = SavedSelection.PaymentMethod(paymentMethod.id.orEmpty())
            )
        )
        scenario.onFragment {
            assertThat(activityViewModel(it).selection.value)
                .isEqualTo(paymentSelection)
        }

        scenario.recreate()
        scenario.onFragment {
            assertThat(activityViewModel(it).selection.value)
                .isEqualTo(paymentSelection)
        }
    }

    @Test
    fun `sets up adapter`() {
        createScenario().onFragment {
            val recycler = recyclerView(it)
            val adapter = recycler.adapter as PaymentOptionsAdapter

            idleLooper()

            assertThat(adapter.itemCount)
                .isEqualTo(4)
        }
    }

    @Test
    fun `updates selection on click`() {
        val savedPaymentMethod = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        createScenario().onFragment {
            val activityViewModel = activityViewModel(it)
            idleLooper()

            val recycler = recyclerView(it)
            assertThat(recycler.adapter).isInstanceOf(PaymentOptionsAdapter::class.java)
            val adapter = recycler.adapter as PaymentOptionsAdapter
            adapter.paymentOptionSelectedListener(savedPaymentMethod, true)
            idleLooper()

            assertThat(activityViewModel.selection.value)
                .isEqualTo(savedPaymentMethod)
        }
    }

    @Test
    fun `posts transition when add card clicked`() {
        createScenario().onFragment {
            val activityViewModel = activityViewModel(it)
            assertThat(activityViewModel.transition.value).isNull()

            idleLooper()

            val recycler = recyclerView(it)
            assertThat(recycler.adapter).isInstanceOf(PaymentOptionsAdapter::class.java)
            val adapter = recycler.adapter as PaymentOptionsAdapter
            adapter.addCardClickListener.onClick(it.requireView())
            idleLooper()

            assertThat(activityViewModel.transition.value)
                .isEqualTo(
                    PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull(
                        FragmentConfigFixtures.DEFAULT.copy(
                            paymentMethods = PAYMENT_METHODS
                        )
                    )
                )
        }
    }

    @Test
    fun `click on GooglePay item should update selection`() {
        createScenario().onFragment { fragment ->
            val activityViewModel = activityViewModel(fragment)
            idleLooper()

            val recycler = recyclerView(fragment)

            val googlePayView = recycler.children.toList()[1]
            googlePayView.performClick()

            assertThat(activityViewModel.selection.value)
                .isEqualTo(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `updateHeader() should update header view`() {
        createScenario().onFragment { fragment ->
            val header = requireNotNull(fragment.view?.findViewById<TextView>(R.id.header))
            assertThat(header.text.toString())
                .isEqualTo("Pay $10.99 using")
        }
    }

    @Test
    fun `started fragment should report onShowExistingPaymentOptions() event`() {
        createScenario().onFragment {
            verify(eventReporter).onShowExistingPaymentOptions()
        }
    }

    @Test
    fun `fragment started without FragmentConfig should emit fatal`() {
        createScenario(
            fragmentConfig = null
        ).onFragment { fragment ->
            assertThat(fragment.sheetViewModel.fatal.value?.message)
                .isEqualTo("Failed to start existing payment options fragment.")
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

    private fun createScenario(
        fragmentConfig: FragmentConfig? = FRAGMENT_CONFIG
    ): FragmentScenario<PaymentSheetPaymentMethodsListFragment> {
        return launchFragmentInContainer<PaymentSheetPaymentMethodsListFragment>(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
            ),
            R.style.StripePaymentSheetDefaultTheme,
            factory = PaymentSheetFragmentFactory(eventReporter)
        )
    }

    private companion object {
        private val PAYMENT_METHODS = listOf(
            PaymentMethod("one", 0, false, PaymentMethod.Type.Card),
            PaymentMethod("two", 0, false, PaymentMethod.Type.Card)
        )

        private val FRAGMENT_CONFIG = FragmentConfigFixtures.DEFAULT.copy(
            paymentMethods = PAYMENT_METHODS
        )
    }
}
