package com.stripe.android.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.PaymentFlowResult
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class PaymentAuthWebViewActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = PaymentBrowserAuthContract()

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `setResult is expected value with default args`() {
        runOnActivityScenario { activityScenario ->
            activityScenario.onActivity {
                it.finish()
            }

            assertThat(contract.parseResult(REQUEST_CODE, activityScenario.result.resultData))
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = CLIENT_SECRET,
                        sourceId = ""
                    )
                )
        }
    }

    @Test
    fun `setResult is expected value when onAuthComplete is invoked with error`() {
        runOnActivityScenario { activityScenario ->
            activityScenario.onActivity {
                it.onAuthComplete(
                    ActivityNotFoundException()
                )
                it.finish()
            }

            assertThat(contract.parseResult(REQUEST_CODE, activityScenario.result.resultData))
                .isEqualTo(
                    PaymentFlowResult.Unvalidated(
                        clientSecret = CLIENT_SECRET,
                        exception = StripeException.create(ActivityNotFoundException()),
                        flowOutcome = StripeIntentResult.Outcome.FAILED,
                        canCancelSource = true,
                        sourceId = ""
                    )
                )
        }
    }

    private fun runOnActivityScenario(
        onActivityScenario: (ActivityScenario<PaymentAuthWebViewActivity>) -> Unit
    ) {
        ActivityScenario.launchActivityForResult<PaymentAuthWebViewActivity>(
            contract.createIntent(
                context,
                ARGS
            )
        ).use {
            onActivityScenario(it)
        }
    }

    private companion object {
        private const val REQUEST_CODE = 1000
        private const val CLIENT_SECRET = "client_secret"

        private val ARGS = PaymentBrowserAuthContract.Args(
            objectId = "pi_1EceMnCRMbs6FrXfCXdF8dnx",
            requestCode = REQUEST_CODE,
            clientSecret = CLIENT_SECRET,
            url = "https://example.com",
            statusBarColor = Color.RED,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            isInstantApp = false
        )
    }
}
