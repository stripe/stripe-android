package com.stripe.android.paymentsheet

import android.os.Looper.getMainLooper
import androidx.appcompat.app.AlertDialog
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.Amount
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
class PaymentSheetListFragmentTest {
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
            idleLooper()

            val adapter = recyclerView(it).adapter as PaymentOptionsAdapter
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

            val adapter = recyclerView(it).adapter as PaymentOptionsAdapter
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
            assertThat(activityViewModel.transition.value?.peekContent()).isNull()

            idleLooper()

            val adapter = recyclerView(it).adapter as PaymentOptionsAdapter
            adapter.addCardClickListener.onClick(it.requireView())
            idleLooper()

            assertThat(activityViewModel.transition.value?.peekContent())
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
    fun `started fragment should report onShowExistingPaymentOptions() event`() {
        createScenario().onFragment {
            verify(eventReporter).onShowExistingPaymentOptions()
        }
    }

    @Test
    fun `fragment created without FragmentConfig should emit fatal`() {
        createScenario(
            fragmentConfig = null,
            initialState = Lifecycle.State.CREATED
        ).onFragment { fragment ->
            assertThat((fragment.sheetViewModel.paymentSheetResult.value as PaymentSheetResult.Failed).error.message)
                .isEqualTo("Failed to start existing payment options fragment.")
        }
    }

    @Test
    fun `total amount label correctly displays amount`() {
        createScenario().onFragment { fragment ->
            shadowOf(getMainLooper()).idle()
            fragment.sheetViewModel._amount.value = Amount(399, "USD")
            val viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(fragment.view!!)

            assertThat(viewBinding.total.text)
                .isEqualTo("Total: $3.99")
        }
    }

    @Test
    fun `total amount label is hidden for SetupIntent`() {
        createScenario(
            FRAGMENT_CONFIG,
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        ).onFragment { fragment ->
            shadowOf(getMainLooper()).idle()
            val viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(fragment.view!!)

            assertThat(viewBinding.total.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `when config has saved payment methods then show options menu`() {
        createScenario().onFragment { fragment ->
            idleLooper()
            assertThat(fragment.hasOptionsMenu()).isTrue()
        }
    }

    @Test
    fun `when config does not have saved payment methods then show no options menu`() {
        createScenario(FragmentConfigFixtures.DEFAULT).onFragment { fragment ->
            idleLooper()
            assertThat(fragment.hasOptionsMenu()).isFalse()
        }
    }

    @Test
    fun `deletePaymentMethod() removes item from adapter`() {
        createScenario().onFragment { fragment ->
            idleLooper()

            val adapter = recyclerView(fragment).adapter as PaymentOptionsAdapter
            assertThat(adapter.itemCount).isEqualTo(4)

            adapter.toggleEditing()
            adapter.paymentMethodDeleteListener(
                adapter.items[3] as PaymentOptionsAdapter.Item.SavedPaymentMethod
            )

            val dialog = ShadowAlertDialog.getShownDialogs().first() as AlertDialog
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

            idleLooper()

            assertThat(adapter.itemCount).isEqualTo(3)
        }
    }

    private fun recyclerView(it: PaymentSheetListFragment) =
        it.requireView().findViewById<RecyclerView>(R.id.recycler)

    private fun activityViewModel(
        fragment: PaymentSheetListFragment
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory(
                { fragment.requireActivity().application },
                { PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY }
            )
        }.value
    }

    private fun createScenario(
        fragmentConfig: FragmentConfig? = FRAGMENT_CONFIG,
        starterArgs: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    ): FragmentScenario<PaymentSheetListFragment> = launchFragmentInContainer(
        bundleOf(
            PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
            PaymentSheetActivity.EXTRA_STARTER_ARGS to starterArgs
        ),
        R.style.StripePaymentSheetDefaultTheme,
        initialState = initialState,
        factory = PaymentSheetFragmentFactory(eventReporter)
    )

    private companion object {
        private val PAYMENT_METHODS = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )

        private val FRAGMENT_CONFIG = FragmentConfigFixtures.DEFAULT.copy(
            paymentMethods = PAYMENT_METHODS
        )
    }
}
