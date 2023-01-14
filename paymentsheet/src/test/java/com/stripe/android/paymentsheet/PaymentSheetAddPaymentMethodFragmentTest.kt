package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.PaymentIntentFactory
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetAddPaymentMethodFragmentTest : PaymentSheetViewModelTestInjection() {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @After
    override fun after() {
        super.after()
    }

    @Test
    fun `started fragment should report onShowNewPaymentOptionForm() event`() {
        createFragment { _, _ ->
            idleLooper()
            verify(eventReporter).onShowNewPaymentOptionForm(any(), any())
        }
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        createFragment(registerInjector = true) { fragment, viewModel ->
            assertThat(fragment.sheetViewModel).isEqualTo(viewModel)
        }
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() =
        runTest(UnconfinedTestDispatcher()) {
            createFragment(registerInjector = false) { fragment, viewModel ->
                assertThat(fragment.sheetViewModel).isNotEqualTo(viewModel)
            }
        }

    private fun createFragment(
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            injectorKey = "testInjectorKeyAddFragmentTest"
        ),
        paymentMethods: List<PaymentMethod> = emptyList(),
        stripeIntent: StripeIntent? = PaymentIntentFixtures.PI_WITH_SHIPPING,
        registerInjector: Boolean = true,
        onReady: (PaymentSheetAddPaymentMethodFragment, PaymentSheetViewModel) -> Unit
    ): FragmentScenario<PaymentSheetAddPaymentMethodFragment> {
        assertThat(WeakMapInjectorRegistry.staticCacheMap.size).isEqualTo(0)
        val viewModel = createViewModel(
            stripeIntent as PaymentIntent,
            customerRepositoryPMs = paymentMethods,
            injectorKey = args.injectorKey
        )
        idleLooper()

        // somehow the saveInstanceState for the viewModel needs to be present

        return launchFragmentInContainer<PaymentSheetAddPaymentMethodFragment>(
            bundleOf(PaymentSheetActivity.EXTRA_STARTER_ARGS to args),
            R.style.StripePaymentSheetDefaultTheme,
            initialState = Lifecycle.State.INITIALIZED
        ).moveToState(Lifecycle.State.CREATED).onFragment {
            if (registerInjector) {
                idleLooper()
                registerViewModel(args.injectorKey, viewModel, lpmRepository, addressRepository)
            } else {
                it.sheetViewModel.lpmResourceRepository.getRepository().update(
                    stripeIntent,
                    null
                )
                idleLooper()
            }
        }.moveToState(Lifecycle.State.STARTED).onFragment { fragment ->
            onReady(
                fragment,
                viewModel
            )
        }
    }

    companion object {
        val addressRepository =
            AddressRepository(ApplicationProvider.getApplicationContext<Context>().resources)
        val lpmRepository =
            LpmRepository(LpmRepository.LpmRepositoryArguments(ApplicationProvider.getApplicationContext<Application>().resources)).apply {
                this.update(
                    PaymentIntentFactory.create(
                        paymentMethodTypes = listOf(
                            PaymentMethod.Type.Card.code,
                            PaymentMethod.Type.USBankAccount.code,
                            PaymentMethod.Type.SepaDebit.code,
                            PaymentMethod.Type.Bancontact.code
                        )
                    ),
                    null
                )
            }
    }
}
