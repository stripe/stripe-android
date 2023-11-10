package com.stripe.android.payments.paymentlauncher

import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentLauncherConfirmationActivityTest {
    private val viewModel = mock<PaymentLauncherViewModel>().also {
        whenever(it.internalPaymentResult).thenReturn(mock())
    }
    private val testFactory = TestUtils.viewModelFactoryFor(viewModel)

    @Test
    fun `statusBarColor is set to transparent on window`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetAuthenticators = false,
                    confirmStripeIntentParams = mock(),
                    statusBarColor = Color.CYAN
                ).toBundle()
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.window.statusBarColor).isEqualTo(Color.TRANSPARENT)
            }
        }
    }

    @Test
    fun `start should call register`() {
        val confirmStripeIntentParams = mock<ConfirmStripeIntentParams>()
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetAuthenticators = false,
                    confirmStripeIntentParams = confirmStripeIntentParams,
                    statusBarColor = Color.RED,
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runTest {
                    verify(viewModel).register(any(), any())
                }
            }
        }
    }

    @Test
    fun `start with IntentConfirmationArgs should confirmStripeIntent`() {
        val confirmStripeIntentParams = mock<ConfirmStripeIntentParams>()
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetAuthenticators = false,
                    confirmStripeIntentParams = confirmStripeIntentParams,
                    statusBarColor = Color.RED,
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runTest {
                    verify(viewModel).confirmStripeIntent(eq(confirmStripeIntentParams), any())
                }
            }
        }
    }

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
                runTest {
                    verify(viewModel).handleNextActionForStripeIntent(eq(CLIENT_SECRET), any())
                }
            }
        }
    }

    @Test
    fun `start with SetupIntentNextActionArgs should handleNextActionForStripeIntent`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetAuthenticators = false,
                    setupIntentClientSecret = CLIENT_SECRET,
                    statusBarColor = Color.RED,
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runTest {
                    verify(viewModel).handleNextActionForStripeIntent(eq(CLIENT_SECRET), any())
                    verify(viewModel).register(any(), any())
                }
            }
        }
    }

    @Test
    fun `start without args should finish with Error result`() {
        mockViewModelActivityScenario().launchForResult(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            )
        ).use { activityScenario ->
            assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result =
                InternalPaymentResult.fromIntent(
                    activityScenario.getResult().resultData
                ) as InternalPaymentResult.Failed
            assertThat(result.throwable.message)
                .isEqualTo(
                    PaymentLauncherConfirmationActivity.EMPTY_ARG_ERROR
                )
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
        const val CLIENT_SECRET = "clientSecret"
        const val TEST_STRIPE_ACCOUNT_ID = "accountId"
        val PRODUCT_USAGE = setOf("TestProductUsage")
        val PAYMENT_INTENT_NEXT_ACTION_ARGS =
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                enableLogging = false,
                productUsage = PRODUCT_USAGE,
                includePaymentSheetAuthenticators = false,
                paymentIntentClientSecret = CLIENT_SECRET,
                statusBarColor = Color.RED,
            )
    }
}
