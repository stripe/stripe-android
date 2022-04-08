package com.stripe.android.paymentsheet

import android.app.Application
import androidx.core.os.bundleOf
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
import com.stripe.android.utils.TestUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsAddPaymentMethodFragmentTest : PaymentOptionsViewModelTestInjection() {

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
    fun `Factory gets initialized by Injector when Injector is available`() {
        createFragment { fragment, _, viewModel ->
            val factory = PaymentOptionsViewModel.Factory(
                { ApplicationProvider.getApplicationContext() },
                {
                    PaymentOptionContract.Args(
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        null,
                        false,
                        null,
                        null,
                        DUMMY_INJECTOR_KEY,
                        false,
                        mock()
                    )
                },
                fragment
            )
            assertThat(fragment.sheetViewModel).isEqualTo(viewModel)

            WeakMapInjectorRegistry.clear()
        }
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runBlockingTest {
        createFragment(registerInjector = false) { fragment, _, viewModel ->
            val context = ApplicationProvider.getApplicationContext<Application>()
            val productUsage = setOf("TestProductUsage")
            PaymentConfiguration.init(context, "testKey")
            val factory = PaymentOptionsViewModel.Factory(
                { context },
                {
                    PaymentOptionContract.Args(
                        PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                        emptyList(),
                        null,
                        false,
                        null,
                        null,
                        DUMMY_INJECTOR_KEY,
                        false,
                        productUsage
                    )
                },
                fragment
            )
            assertThat(fragment.sheetViewModel).isNotEqualTo(viewModel)
        }
    }

    private fun createFragment(
        args: PaymentOptionContract.Args = PaymentOptionContract.Args(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            paymentMethods = emptyList(),
            config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
            isGooglePayReady = false,
            newCard = null,
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
            injectorKey = DUMMY_INJECTOR_KEY,
            enableLogging = false,
            productUsage = mock()
        ),
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        registerInjector: Boolean = true,
        onReady: (PaymentOptionsAddPaymentMethodFragment, FragmentPaymentsheetAddPaymentMethodBinding, PaymentOptionsViewModel) -> Unit
    ) {
        assertThat(WeakMapInjectorRegistry.staticCacheMap.size).isEqualTo(0)
        val viewModel = createViewModel(
            paymentMethods = args.paymentMethods,
            injectorKey = args.injectorKey,
            args = args
        )
        viewModel.setStripeIntent(args.stripeIntent)
        TestUtils.idleLooper()
        if (registerInjector) {
            registerViewModel(args.injectorKey, viewModel, createFormViewModel())
        }
        launchFragmentInContainer<PaymentOptionsAddPaymentMethodFragment>(
            bundleOf(
                PaymentOptionsActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentOptionsActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme,
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
