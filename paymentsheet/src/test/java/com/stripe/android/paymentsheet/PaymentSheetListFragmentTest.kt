package com.stripe.android.paymentsheet

import android.os.Looper.getMainLooper
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.fragment.app.testing.withFragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheetViewModelInjectionTest.Companion.addressRepository
import com.stripe.android.paymentsheet.PaymentSheetViewModelInjectionTest.Companion.lpmRepository
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_GOOGLE_PAY_STATE
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PAYMENT_METHODS
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_SAVED_SELECTION
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetListFragmentTest : BasePaymentSheetViewModelInjectionTest() {
    @InjectorKey
    private val injectorKey: String = "PaymentSheetListFragmentTest"

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
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.initializePaymentOptions(paymentMethods = listOf(paymentMethod))
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
    fun `sets up adapter`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.initializePaymentOptions(
                isGooglePayReady = GooglePayState.NotAvailable,
            )
        }.moveToState(Lifecycle.State.RESUMED).onFragment {
            idleLooper()

            val adapter = recyclerView(it).adapter as PaymentOptionsAdapter
            assertThat(adapter.itemCount)
                .isEqualTo(3)
        }
    }

    @Test
    @Config(qualifiers = "w320dp")
    fun `when screen is 320dp wide, adapter should show 2 and a half items with 114dp width`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.initializePaymentOptions()
        }.moveToState(Lifecycle.State.RESUMED).onFragment {
            val item = recyclerView(it).layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(114)
        }
    }

    @Test
    @Config(qualifiers = "w481dp")
    fun `when screen is 481dp wide, adapter should show 3 and a half items with 128dp width`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.initializePaymentOptions()
        }.moveToState(Lifecycle.State.RESUMED).onFragment {
            val item = recyclerView(it).layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(128)
        }
    }

    @Test
    @Config(qualifiers = "w482dp")
    fun `when screen is 482dp wide, adapter should show 4 items with 112dp width`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment { fragment ->
            fragment.initializePaymentOptions()
        }.moveToState(Lifecycle.State.RESUMED).onFragment {
            val item = recyclerView(it).layoutManager!!.findViewByPosition(0)
            assertThat(item!!.measuredWidth).isEqualTo(112)
        }
    }

    @Test
    fun `updates selection on click`() {
        val savedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val selectedItem = PaymentOptionsItem.SavedPaymentMethod(
            displayName = "Card",
            paymentMethod = savedPaymentMethod,
        )

        createScenario().onFragment {
            val activityViewModel = activityViewModel(it)
            idleLooper()

            val adapter = recyclerView(it).adapter as PaymentOptionsAdapter
            adapter.paymentOptionSelected(selectedItem)
            idleLooper()

            assertThat(activityViewModel.selection.value)
                .isEqualTo(PaymentSelection.Saved(savedPaymentMethod))
        }
    }

    @Test
    fun `posts transition when add card clicked`() = runTest {
        val scenario = createScenario()

        val viewModel = scenario.withFragment { activityViewModel(this) }
        val recyclerView = scenario.withFragment { recyclerView(this) }

        viewModel.currentScreen.test {
            assertThat(awaitItem()).isEqualTo(SelectSavedPaymentMethods)

            val adapter = recyclerView.adapter as PaymentOptionsAdapter
            adapter.addCardClickListener()

            assertThat(awaitItem()).isEqualTo(AddAnotherPaymentMethod)
        }
    }

    @Test
    fun `total amount label correctly displays amount`() {
        createScenario().onFragment { fragment ->
            shadowOf(getMainLooper()).idle()

            assertThat(fragment.sheetViewModel.isProcessingPaymentIntent).isTrue()
        }
    }

    @Test
    fun `total amount label is hidden for SetupIntent`() {
        createScenario(
            starterArgs = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        ).onFragment { fragment ->
            shadowOf(getMainLooper()).idle()

            assertThat(fragment.sheetViewModel.isProcessingPaymentIntent).isFalse()
        }
    }

    @Test
    fun `when config has saved payment methods then show options menu`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED,
            paymentMethods = PAYMENT_METHODS
        ).moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            assertThat(fragment.hasOptionsMenu()).isTrue()
        }
    }

    @Test
    fun `when config does not have saved payment methods then show no options menu`() {
        createScenario(
            initialState = Lifecycle.State.INITIALIZED,
            paymentMethods = emptyList()
        ).moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            fragment.initializePaymentOptions()
            idleLooper()
            assertThat(fragment.hasOptionsMenu()).isFalse()
        }
    }

    private fun recyclerView(it: PaymentSheetListFragment) =
        it.requireView().findViewById<RecyclerView>(R.id.recycler)

    private fun activityViewModel(
        fragment: PaymentSheetListFragment
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory { PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY }
        }.value
    }

    private fun createScenario(
        starterArgs: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            injectorKey = injectorKey
        ),
        initialState: Lifecycle.State = Lifecycle.State.RESUMED,
        paymentMethods: List<PaymentMethod> = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
    ): FragmentScenario<PaymentSheetListFragment> {
        assertThat(WeakMapInjectorRegistry.retrieve(injectorKey)).isNull()
        createViewModel(
            stripeIntent = stripeIntent,
            customerRepositoryPMs = paymentMethods,
            injectorKey = starterArgs.injectorKey,
            args = starterArgs
        ).apply {
            idleLooper()
            registerViewModel(starterArgs.injectorKey, this, lpmRepository, addressRepository)
        }
        return launchFragmentInContainer(
            bundleOf(PaymentSheetActivity.EXTRA_STARTER_ARGS to starterArgs),
            R.style.StripePaymentSheetDefaultTheme,
            initialState = initialState
        )
    }

    private fun PaymentSheetListFragment.initializePaymentOptions(
        paymentMethods: List<PaymentMethod> = PAYMENT_METHODS,
        isGooglePayReady: GooglePayState = GooglePayState.NotAvailable,
        savedSelection: SavedSelection = SavedSelection.None,
    ) {
        sheetViewModel.savedStateHandle[SAVE_PAYMENT_METHODS] = paymentMethods
        sheetViewModel.savedStateHandle[SAVE_GOOGLE_PAY_STATE] = isGooglePayReady
        sheetViewModel.savedStateHandle[SAVE_SAVED_SELECTION] = savedSelection
    }

    private companion object {
        private val PAYMENT_METHODS = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
    }
}
