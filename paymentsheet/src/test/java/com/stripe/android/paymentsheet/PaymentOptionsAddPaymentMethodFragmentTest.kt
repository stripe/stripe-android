package com.stripe.android.paymentsheet

import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.TestUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsAddPaymentMethodFragmentTest : PaymentOptionsViewModelTestInjection() {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val lpmRepository = LpmRepository(context.resources)

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @After
    fun cleanUp() {
        super.after()
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() = runTest(UnconfinedTestDispatcher()) {
        createFragment { fragment, _, viewModel ->
            assertThat(fragment.sheetViewModel).isEqualTo(viewModel)
        }
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runTest(UnconfinedTestDispatcher()) {
        createFragment(registerInjector = false) { fragment, _, viewModel ->
            assertThat(fragment.sheetViewModel).isNotEqualTo(viewModel)
        }
    }

    private fun createFragment(
        args: PaymentOptionContract.Args = PaymentOptionContract.Args(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
            isGooglePayReady = false,
            newLpm = null,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
            injectorKey = DUMMY_INJECTOR_KEY,
            enableLogging = false,
            productUsage = mock()
        ),
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        registerInjector: Boolean = true,
        onReady: (PaymentOptionsAddPaymentMethodFragment, FragmentPaymentsheetAddPaymentMethodBinding, PaymentOptionsViewModel) -> Unit
    ): FragmentScenario<PaymentOptionsAddPaymentMethodFragment> {
        assertThat(WeakMapInjectorRegistry.staticCacheMap.size).isEqualTo(0)
        val viewModel = createViewModel(
            paymentMethods = args.paymentMethods,
            injectorKey = args.injectorKey,
            args = args
        )
        viewModel.setStripeIntent(args.stripeIntent)
        TestUtils.idleLooper()

        if (registerInjector) {
            registerViewModel(args.injectorKey, viewModel, lpmRepository)
        }
        return launchFragmentInContainer<PaymentOptionsAddPaymentMethodFragment>(
            bundleOf(
                PaymentOptionsActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentOptionsActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme
        ).onFragment { fragment ->
            onReady(
                fragment,
                FragmentPaymentsheetAddPaymentMethodBinding.bind(
                    requireNotNull(fragment.view)
                ),
                viewModel
            )
        }
    }
}
