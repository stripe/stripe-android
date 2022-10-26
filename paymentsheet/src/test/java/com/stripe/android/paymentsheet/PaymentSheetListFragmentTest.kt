package com.stripe.android.paymentsheet

import android.content.Context
import android.os.Looper.getMainLooper
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetListFragmentTest : PaymentSheetViewModelTestInjection() {
    @InjectorKey
    private val injectorKey: String = "PaymentSheetListFragmentTest"

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
    fun `When last item is deleted then edit menu item is hidden`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        createScenario(
            fragmentConfig = FRAGMENT_CONFIG.copy(
                savedSelection = SavedSelection.PaymentMethod(paymentMethod.id.orEmpty())
            )
        ).onFragment { fragment ->
            fragment.sheetViewModel._paymentMethods.postValue(emptyList())
            assertThat(fragment.sheetViewModel.isEditing.value).isFalse()
        }
    }

    @Test
    fun `started fragment should report onShowExistingPaymentOptions() event`() {
        createScenario().onFragment {
            verify(eventReporter).onShowExistingPaymentOptions(any(), any())
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

            assertThat(fragment.sheetViewModel.isProcessingPaymentIntent).isTrue()
        }
    }

    @Test
    fun `total amount label is hidden for SetupIntent`() {
        createScenario(
            FRAGMENT_CONFIG.copy(stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD),
            PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        ).onFragment { fragment ->
            shadowOf(getMainLooper()).idle()

            assertThat(fragment.sheetViewModel.isProcessingPaymentIntent).isFalse()
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

//    private fun recyclerView(it: PaymentSheetListFragment) =
//        it.requireView().findViewById<RecyclerView>(R.id.recycler)

    private fun activityViewModel(
        fragment: PaymentSheetListFragment
    ): PaymentSheetViewModel {
        return fragment.activityViewModels<PaymentSheetViewModel> {
            PaymentSheetViewModel.Factory { PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY }
        }.value
    }

    @OptIn(FlowPreview::class)
    private fun createScenario(
        fragmentConfig: FragmentConfig? = FRAGMENT_CONFIG,
        starterArgs: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            injectorKey = injectorKey
        ),
        initialState: Lifecycle.State = Lifecycle.State.RESUMED,
        paymentMethods: List<PaymentMethod> = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    ): FragmentScenario<PaymentSheetListFragment> {
        assertThat(WeakMapInjectorRegistry.retrieve(injectorKey)).isNull()
        fragmentConfig?.let {
            createViewModel(
                fragmentConfig.stripeIntent,
                customerRepositoryPMs = paymentMethods,
                injectorKey = starterArgs.injectorKey,
                args = starterArgs
            ).apply {
                updatePaymentMethods(fragmentConfig.stripeIntent)
                setStripeIntent(fragmentConfig.stripeIntent)
                idleLooper()
                registerViewModel(starterArgs.injectorKey, this, lpmRepository, addressRepository)
            }
        }
        return launchFragmentInContainer(
            bundleOf(
                PaymentSheetActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentSheetActivity.EXTRA_STARTER_ARGS to starterArgs
            ),
            R.style.StripePaymentSheetDefaultTheme,
            initialState = initialState
        )
    }

    private fun PaymentSheetListFragment.initializePaymentOptions(
        paymentMethods: List<PaymentMethod> = PAYMENT_METHODS,
        isGooglePayReady: Boolean = false,
        isLinkEnabled: Boolean = false,
        savedSelection: SavedSelection = SavedSelection.None,
    ) {
        sheetViewModel._paymentMethods.value = paymentMethods
        sheetViewModel._isGooglePayReady.value = isGooglePayReady
        sheetViewModel._isLinkEnabled.value = isLinkEnabled
        sheetViewModel.savedStateHandle["saved_selection"] = savedSelection
    }

    private companion object {
        private val PAYMENT_METHODS = listOf(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        private val FRAGMENT_CONFIG = FragmentConfigFixtures.DEFAULT

        val addressRepository = AddressRepository(ApplicationProvider.getApplicationContext<Context>().resources)
        val lpmRepository =
            LpmRepository(
                LpmRepository.LpmRepositoryArguments(
                    InstrumentationRegistry.getInstrumentation().targetContext.resources
                )
            ).apply {
                this.updateFromDisk()
            }
    }
}
