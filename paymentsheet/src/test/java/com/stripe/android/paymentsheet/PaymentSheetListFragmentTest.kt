package com.stripe.android.paymentsheet

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
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
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_GOOGLE_PAY_STATE
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PAYMENT_METHODS
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_SAVED_SELECTION
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
