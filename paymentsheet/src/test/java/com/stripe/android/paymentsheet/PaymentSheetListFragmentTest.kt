package com.stripe.android.paymentsheet

import android.os.Looper.getMainLooper
import androidx.appcompat.app.AlertDialog
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
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetPaymentMethodsListBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetListFragmentTest : PaymentSheetViewModelTestInjection() {

    @After
    override fun after() {
        super.after()
    }

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `recovers payment method selection when shown`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(paymentMethod)

        val scenario = createScenario(
            fragmentConfig = FRAGMENT_CONFIG.copy(
                isGooglePayReady = true,
                savedSelection = SavedSelection.PaymentMethod(paymentMethod.id.orEmpty())
            ),
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.sheetViewModel._paymentMethods.value = listOf(paymentMethod)
        }.moveToState(Lifecycle.State.STARTED).onFragment {
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
    fun `recovers edit state when shown`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        createScenario(
            fragmentConfig = FRAGMENT_CONFIG.copy(
                isGooglePayReady = true,
                savedSelection = SavedSelection.PaymentMethod(paymentMethod.id.orEmpty())
            ),
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.sheetViewModel._paymentMethods.value = listOf(paymentMethod)
        }.moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            assertThat(fragment.isEditing).isFalse()
            fragment.isEditing = true
        }.recreate().onFragment {
            assertThat(it.isEditing).isTrue()
        }
    }

    @Test
    fun `sets up adapter`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.sheetViewModel._paymentMethods.value = PAYMENT_METHODS
        }.moveToState(Lifecycle.State.STARTED).onFragment {
            idleLooper()

            val adapter = recyclerView(it).adapter as PaymentOptionsAdapter
            assertThat(adapter.itemCount)
                .isEqualTo(4)
        }
    }

    @Test
    @Config(qualifiers = "w320dp")
    fun `when screen is 320dp wide, adapter should show 2 and a half items with 114dp width`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.sheetViewModel._paymentMethods.value = PAYMENT_METHODS
        }.moveToState(Lifecycle.State.STARTED).onFragment {
            val item = recyclerView(it).layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(114)
        }
    }

    @Test
    @Config(qualifiers = "w481dp")
    fun `when screen is 481dp wide, adapter should show 3 and a half items with 127dp width`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.sheetViewModel._paymentMethods.value = PAYMENT_METHODS
        }.moveToState(Lifecycle.State.STARTED).onFragment {
            val item = recyclerView(it).layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(127)
        }
    }

    @Test
    @Config(qualifiers = "w482dp")
    fun `when screen is 482dp wide, adapter should show 4 items with 112dp width`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.sheetViewModel._paymentMethods.value = PAYMENT_METHODS
        }.moveToState(Lifecycle.State.STARTED).onFragment {
            val item = recyclerView(it).layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(112)
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
                        FragmentConfigFixtures.DEFAULT
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
            fragment.sheetViewModel.setStripeIntent(
                PaymentIntentFixtures.PI_OFF_SESSION.copy(
                    amount = 399
                )
            )
            val viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(fragment.view!!)

            assertThat(viewBinding.total.text)
                .isEqualTo("Total: $3.99")
        }
    }

    @Test
    fun `total amount label is hidden for SetupIntent`() {
        createScenario(
            FRAGMENT_CONFIG.copy(stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD),
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP,
        ).onFragment { fragment ->
            shadowOf(getMainLooper()).idle()
            val viewBinding = FragmentPaymentsheetPaymentMethodsListBinding.bind(fragment.view!!)

            assertThat(viewBinding.total.isVisible)
                .isFalse()
        }
    }

    @Test
    fun `when config has saved payment methods then show options menu`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED,
            paymentMethods = PAYMENT_METHODS
        ).moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            idleLooper()
            assertThat(fragment.hasOptionsMenu()).isTrue()
        }
    }

    @Test
    fun `when config does not have saved payment methods then show no options menu`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED,
            paymentMethods = emptyList()
        ).moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            idleLooper()
            assertThat(fragment.hasOptionsMenu()).isFalse()
        }
    }

    @Test
    fun `deletePaymentMethod() removes item from adapter`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED,
            paymentMethods = PAYMENT_METHODS
        ).moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            idleLooper()

            val adapter = recyclerView(fragment).adapter as PaymentOptionsAdapter
            assertThat(adapter.itemCount).isEqualTo(4)

            fragment.isEditing = true
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
                { PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY },
                mock(),
            )
        }.value
    }

    private fun createScenario(
        fragmentConfig: FragmentConfig? = FRAGMENT_CONFIG,
        starterArgs: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        initialState: Lifecycle.State = Lifecycle.State.RESUMED,
        paymentMethods: List<PaymentMethod> = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    ): FragmentScenario<PaymentSheetListFragment> {

        fragmentConfig?.let {
            createViewModel(
                fragmentConfig.stripeIntent,
                paymentMethods = paymentMethods,
                injectorKey = starterArgs.injectorKey,
                args = starterArgs
            ).apply {
                updatePaymentMethods(fragmentConfig.stripeIntent)
                setStripeIntent(fragmentConfig.stripeIntent)
                idleLooper()
                registerViewModel(this, starterArgs.injectorKey)
            }
        }
        return launchFragmentInContainer(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to starterArgs
            ),
            R.style.StripePaymentSheetDefaultTheme,
            initialState = initialState,
        )
    }

    private companion object {
        private val PAYMENT_METHODS = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )

        private val FRAGMENT_CONFIG = FragmentConfigFixtures.DEFAULT
    }
}
