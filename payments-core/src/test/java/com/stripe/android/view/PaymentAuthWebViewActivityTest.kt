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
import com.stripe.android.R
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

    @Test
    fun `cancelIntentSource sets canceled result for 3DS URL with setatt source`() {
        val args3ds = ARGS.copy(
            url = "https://hooks.stripe.com/redirect/authenticate/src_1234/setatt_abc",
            clientSecret = "seti_xyz_secret_123",
            shouldCancelSource = true
        )
        ActivityScenario.launchActivityForResult<PaymentAuthWebViewActivity>(
            contract.createIntent(context, args3ds)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.onOptionsItemSelected(
                    activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
                        .menu.findItem(R.id.action_close)
                )
            }
            val resultIntent = contract.parseResult(REQUEST_CODE, activityScenario.result.resultData)
            assertThat(resultIntent.flowOutcome)
                .isEqualTo(StripeIntentResult.Outcome.CANCELED)
            assertThat(resultIntent.canCancelSource)
                .isTrue()
        }
    }

    @Test
    fun `cancelIntentSource sets canceled result for non-3DS URL without network call`() {
        val argsNon3ds = ARGS.copy(
            url = "https://example.com/some-other-flow",
            clientSecret = "pi_abc_secret_456",
            shouldCancelSource = true
        )
        ActivityScenario.launchActivityForResult<PaymentAuthWebViewActivity>(
            contract.createIntent(context, argsNon3ds)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.onOptionsItemSelected(
                    activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
                        .menu.findItem(R.id.action_close)
                )
            }
            val resultIntent = contract.parseResult(REQUEST_CODE, activityScenario.result.resultData)
            assertThat(resultIntent.flowOutcome)
                .isEqualTo(StripeIntentResult.Outcome.CANCELED)
            assertThat(resultIntent.canCancelSource)
                .isTrue()
        }
    }

    @Test
    fun `cancelIntentSource handles payment intent URL with src_ prefix`() {
        val argsPi = ARGS.copy(
            url = "https://hooks.stripe.com/redirect/authenticate/src_5678/src_9xyz",
            clientSecret = "pi_abc_secret_456",
            shouldCancelSource = true
        )
        ActivityScenario.launchActivityForResult<PaymentAuthWebViewActivity>(
            contract.createIntent(context, argsPi)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.onOptionsItemSelected(
                    activity.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
                        .menu.findItem(R.id.action_close)
                )
            }
            val resultIntent = contract.parseResult(REQUEST_CODE, activityScenario.result.resultData)
            assertThat(resultIntent.flowOutcome)
                .isEqualTo(StripeIntentResult.Outcome.CANCELED)
            assertThat(resultIntent.canCancelSource)
                .isTrue()
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
