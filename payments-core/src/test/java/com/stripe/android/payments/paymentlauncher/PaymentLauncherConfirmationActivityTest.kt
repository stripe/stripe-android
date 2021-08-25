package com.stripe.android.payments.paymentlauncher

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.stripe.android.model.ConfirmStripeIntentParams
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
                PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                    INJECTOR_KEY,
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
            Truth.assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result =
                PaymentResult.fromIntent(activityScenario.getResult().resultData) as PaymentResult.Failed
            Truth.assertThat(result.throwable.message)
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
        const val INJECTOR_KEY = 1
        const val CLIENT_SECRET = "clientSecret"
    }
}
