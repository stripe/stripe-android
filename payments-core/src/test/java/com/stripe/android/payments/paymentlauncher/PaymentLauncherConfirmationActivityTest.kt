package com.stripe.android.payments.paymentlauncher

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentLauncherConfirmationActivityTest {
    private val viewModel = mock<PaymentLauncherViewModel>().also {
        whenever(it.paymentLauncherResult).thenReturn(mock())
    }
    private val testFactory = TestUtils.viewModelFactoryFor(viewModel)

    @ExperimentalCoroutinesApi
    @Test
    fun `start with IntentConfirmationArgs should confirmStripeIntent`() {
        val confirmStripeIntentParams = mock<ConfirmStripeIntentParams>()
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    INJECTOR_KEY,
                    ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    TEST_STRIPE_ACCOUNT_ID,
                    false,
                    PRODUCT_USAGE,
                    confirmStripeIntentParams
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runBlockingTest {
                    verify(viewModel).confirmStripeIntent(confirmStripeIntentParams)
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `start with PaymentIntentNextActionArgs should handleNextActionForStripeIntent`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PAYMENT_INTENT_NEXT_ACTION_ARGS.toBundle()
            )
        ).use {
            it.onActivity {
                runBlockingTest {
                    verify(viewModel).handleNextActionForStripeIntent(CLIENT_SECRET)
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `start with SetupIntentNextActionArgs should handleNextActionForStripeIntent`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                    INJECTOR_KEY,
                    ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    TEST_STRIPE_ACCOUNT_ID,
                    false,
                    PRODUCT_USAGE,
                    CLIENT_SECRET
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runBlockingTest {
                    verify(viewModel).handleNextActionForStripeIntent(CLIENT_SECRET)
                }
            }
        }
    }

    @Test
    fun `start without args should finish with Error result`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            )
        ).use { activityScenario ->
            assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result =
                PaymentResult.fromIntent(activityScenario.getResult().resultData) as PaymentResult.Failed
            assertThat(result.throwable.message)
                .isEqualTo(
                    PaymentLauncherConfirmationActivity.EMPTY_ARG_ERROR
                )
        }
    }

    @Test
    fun `viewModelFactory gets initialized by Injector when Injector is available`() {
        val injector = object : Injector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as PaymentLauncherViewModel.Factory
                factory.stripeApiRepository = mock()
                factory.authenticatorRegistry = mock()
                factory.defaultReturnUrl = mock()
                factory.apiRequestOptionsProvider = mock()
                factory.threeDs1IntentReturnUrlMap = mock()
                factory.lazyPaymentIntentFlowResultProcessor = mock()
                factory.lazySetupIntentFlowResultProcessor = mock()
                factory.analyticsRequestExecutor = mock()
                factory.analyticsRequestFactory = mock()
                factory.uiContext = mock()
            }
        }
        WeakMapInjectorRegistry.register(injector, INJECTOR_KEY)

        ActivityScenario.launch<PaymentLauncherConfirmationActivity>(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PAYMENT_INTENT_NEXT_ACTION_ARGS.toBundle()
            )
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
        }
        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    @Test
    fun `viewModelFactory gets initialized with fallback when no Injector is available`() {
        ActivityScenario.launch<PaymentLauncherConfirmationActivity>(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PAYMENT_INTENT_NEXT_ACTION_ARGS.toBundle()
            )
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    private fun mockViewModelActivityScenario():
        InjectableActivityScenario<PaymentLauncherConfirmationActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = testFactory
            }
        }
    }

    private companion object {
        const val INJECTOR_KEY = 1
        const val CLIENT_SECRET = "clientSecret"
        const val TEST_STRIPE_ACCOUNT_ID = "accountId"
        val PRODUCT_USAGE = setOf("TestProductUsage")
        val PAYMENT_INTENT_NEXT_ACTION_ARGS =
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                INJECTOR_KEY,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                TEST_STRIPE_ACCOUNT_ID,
                false,
                PRODUCT_USAGE,
                CLIENT_SECRET
            )
    }
}
