package com.stripe.android.paymentsheet

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.databinding.FragmentPaymentsheetAddPaymentMethodBinding
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.FragmentConfigFixtures
import com.stripe.android.paymentsheet.ui.PaymentSheetFragmentFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentOptionsAddPaymentMethodFragmentTest {
    private val eventReporter = mock<EventReporter>()

    private val testInjector: Injector = object : Injector {
        override fun inject(injectable: Injectable) {
            val factory = (injectable as PaymentOptionsViewModel.Factory)
            factory.eventReporter = eventReporter
            factory.workContext = TestCoroutineDispatcher()
            factory.prefsRepositoryFactory = { mock() }
        }
    }

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
        WeakMapInjectorRegistry.register(testInjector, MOCK_INJECTOR_KEY)
    }

    @After
    fun cleanUp() {
        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    @Test
    fun `when isGooglePayEnabled=true should still not display the Google Pay button`() {
        createFragment { _, viewBinding ->
            assertThat(viewBinding.googlePayButton.isVisible)
                .isFalse()
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
            injectorKey = MOCK_INJECTOR_KEY
        ),
        fragmentConfig: FragmentConfig? = FragmentConfigFixtures.DEFAULT,
        onReady: (PaymentOptionsAddPaymentMethodFragment, FragmentPaymentsheetAddPaymentMethodBinding) -> Unit
    ) {
        launchFragmentInContainer<PaymentOptionsAddPaymentMethodFragment>(
            bundleOf(
                PaymentOptionsActivity.EXTRA_FRAGMENT_CONFIG to fragmentConfig,
                PaymentOptionsActivity.EXTRA_STARTER_ARGS to args
            ),
            R.style.StripePaymentSheetDefaultTheme,
            factory = PaymentSheetFragmentFactory(eventReporter)
        ).onFragment { fragment ->
            onReady(
                fragment,
                FragmentPaymentsheetAddPaymentMethodBinding.bind(
                    requireNotNull(fragment.view)
                )
            )
        }
    }

    companion object {
        private const val MOCK_INJECTOR_KEY = 0
    }
}
