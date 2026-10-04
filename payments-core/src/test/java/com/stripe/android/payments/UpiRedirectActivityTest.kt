package com.stripe.android.payments

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.R
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.ApiConfiguration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class UpiRedirectActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `no installed UPI app finishes with an error`() = runScenario(installApp = false) { scenario ->
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.result.resultData)
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception?.message).isEqualTo(application.getString(R.string.stripe_upi_no_compatible_apps))
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun `missing UPI URL finishes with an error instead of opening a fallback`() = runScenario(
        installApp = false,
        args = ARGS.copy(url = ""),
    ) { scenario ->
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.result.resultData)
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception).isNotNull()
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun `null canceled result still returns to API verification`() = runScenario { scenario ->
        scenario.onActivity { activity ->
            val launchedIntent = shadowOf(activity).nextStartedActivityForResult.intent
            assertThat(launchedIntent.action).isEqualTo(Intent.ACTION_CHOOSER)
            shadowOf(activity).receiveResult(launchedIntent, Activity.RESULT_CANCELED, null)
        }
        assertUnknownOutcome(scenario)
    }

    @Test
    fun `UPI success response does not become SDK payment success`() = runScenario { scenario ->
        scenario.onActivity { activity ->
            val launchedIntent = shadowOf(activity).nextStartedActivityForResult.intent
            shadowOf(activity).receiveResult(
                launchedIntent,
                Activity.RESULT_OK,
                Intent().putExtra("response", "txnId=123&Status=SUCCESS&responseCode=00"),
            )
        }
        assertUnknownOutcome(scenario)
    }

    @Test
    fun `manual return without activity result starts API verification`() = runScenario { scenario ->
        scenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivityForResult.intent.action).isEqualTo(Intent.ACTION_CHOOSER)
        }
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertUnknownOutcome(scenario)
    }

    @Test
    fun `recreation does not launch the chooser again`() = runScenario { scenario ->
        scenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivityForResult.intent.action).isEqualTo(Intent.ACTION_CHOOSER)
        }
        assertThat(shadowOf(application).nextStartedActivity.action).isEqualTo(Intent.ACTION_CHOOSER)
        scenario.recreate()
        assertUnknownOutcome(scenario)
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    private fun assertUnknownOutcome(scenario: ActivityScenario<StripeBrowserLauncherActivity>) {
        val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.result.resultData)
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
        assertThat(result.exception).isNull()
        assertThat(result.clientSecret).isEqualTo(ARGS.clientSecret)
        assertThat(result.stripeAccountId).isEqualTo("acct_upi")
        assertThat(result.canCancelSource).isFalse()
    }

    private fun runScenario(
        installApp: Boolean = true,
        args: PaymentBrowserAuthContract.Args = ARGS,
        block: (ActivityScenario<StripeBrowserLauncherActivity>) -> Unit,
    ) {
        if (installApp) {
            val target = Intent(Intent.ACTION_VIEW, Uri.parse(ARGS.url))
            val info = ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = "test.upi"
                    name = "test.upi.PaymentActivity"
                    exported = true
                    applicationInfo = ApplicationInfo().apply { packageName = "test.upi" }
                }
            }
            shadowOf(application.packageManager).addResolveInfoForIntent(target, info)
        }
        val intent = PaymentBrowserAuthContract().createIntent(application, args)
        ActivityScenario.launchActivityForResult<StripeBrowserLauncherActivity>(intent).use(block)
    }

    private companion object {
        val ARGS = PaymentBrowserAuthContract.Args(
            objectId = "pi_upi",
            requestCode = 50000,
            clientSecret = "pi_upi_secret_123",
            url = "upi://pay?pa=merchant@upi&am=100.00&cu=INR",
            apiConfiguration = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_upi"),
            statusBarColor = null,
            isInstantApp = false,
            shouldUseAppChooser = true,
            shouldCancelIntentOnUserNavigation = false,
        )
    }
}
