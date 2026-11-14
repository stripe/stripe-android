package com.stripe.android.paymentsheet.paymentdatacollection.upi

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
import com.stripe.android.StripeIntentResult
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class UpiAppChooserActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `no installed UPI app fails without launching fallback`() = runScenario(appCount = 0) { scenario ->
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.result.resultData)
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception?.message).isEqualTo(application.getString(R.string.stripe_upi_no_compatible_apps))
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun `missing URL fails without launching fallback`() = runScenario(
        args = UpiFixtures.ARGS.copy(mobileAuthUrl = null),
    ) { scenario ->
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.result.resultData)
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception).isNotNull()
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun `missing arguments fail without crashing`() {
        val intent = Intent(application, UpiAppChooserActivity::class.java)
        ActivityScenario.launchActivityForResult<UpiAppChooserActivity>(intent).use { scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = PaymentFlowResult.Unvalidated.fromIntent(scenario.result.resultData)
            assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
            assertThat(result.exception).isNotNull()
        }
    }

    @Test
    fun `single compatible app still uses the system chooser`() = runScenario(appCount = 1) { scenario ->
        scenario.onActivity { activity ->
            assertThat(shadowOf(activity).nextStartedActivityForResult.intent.action).isEqualTo(Intent.ACTION_CHOOSER)
        }
    }

    @Test
    fun `multiple compatible apps use an unrestricted chooser`() = runScenario(appCount = 3) { scenario ->
        scenario.onActivity { activity ->
            val chooser = shadowOf(activity).nextStartedActivityForResult.intent
            val target = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
            assertThat(target.`package`).isNull()
            assertThat(target.component).isNull()
        }
    }

    @Test
    fun `null canceled app result still requests verification`() = runScenario { scenario ->
        scenario.onActivity { activity ->
            val chooser = shadowOf(activity).nextStartedActivityForResult.intent
            shadowOf(activity).receiveResult(chooser, Activity.RESULT_CANCELED, null)
        }
        assertVerificationResult(scenario)
    }

    @Test
    fun `claimed bank success does not become SDK success`() = runScenario { scenario ->
        scenario.onActivity { activity ->
            val chooser = shadowOf(activity).nextStartedActivityForResult.intent
            shadowOf(activity).receiveResult(
                chooser,
                Activity.RESULT_OK,
                Intent().putExtra("response", "txnId=123&Status=SUCCESS&responseCode=00"),
            )
        }
        assertVerificationResult(scenario)
    }

    @Test
    fun `bank failure response still requests verification`() = runScenario { scenario ->
        scenario.onActivity { activity ->
            val chooser = shadowOf(activity).nextStartedActivityForResult.intent
            shadowOf(activity).receiveResult(
                chooser,
                Activity.RESULT_OK,
                Intent().putExtra("response", "Status=FAILURE"),
            )
        }
        assertVerificationResult(scenario)
    }

    @Test
    fun `manual return without result requests verification`() = runScenario { scenario ->
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertVerificationResult(scenario)
    }

    @Test
    fun `recreation alone neither relaunches chooser nor finishes flow`() = runScenario { scenario ->
        assertThat(shadowOf(application).nextStartedActivity.action).isEqualTo(Intent.ACTION_CHOOSER)
        scenario.recreate()
        assertThat(scenario.state).isEqualTo(Lifecycle.State.RESUMED)
        assertThat(shadowOf(application).nextStartedActivity).isNull()
    }

    @Test
    fun `recreation while away waits for foreground before verification`() = runScenario { scenario ->
        assertThat(shadowOf(application).nextStartedActivity.action).isEqualTo(Intent.ACTION_CHOOSER)
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.recreate()
        assertThat(scenario.state).isEqualTo(Lifecycle.State.CREATED)
        assertThat(shadowOf(application).nextStartedActivity).isNull()
        scenario.moveToState(Lifecycle.State.RESUMED)
        assertVerificationResult(scenario)
    }

    @Test
    fun `back requests verification instead of canceling the payment`() = runScenario { scenario ->
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertVerificationResult(scenario)
    }

    private fun assertVerificationResult(scenario: ActivityScenario<UpiAppChooserActivity>) {
        assertThat(PaymentFlowResult.Unvalidated.fromIntent(scenario.result.resultData)).isEqualTo(
            PaymentFlowResult.Unvalidated(
                clientSecret = UpiFixtures.ARGS.clientSecret,
                flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                stripeAccountId = "acct_upi",
            )
        )
    }

    private fun runScenario(
        appCount: Int = 1,
        args: UpiAppChooserContract.Args = UpiFixtures.ARGS,
        block: (ActivityScenario<UpiAppChooserActivity>) -> Unit,
    ) {
        val target = Intent(Intent.ACTION_VIEW, Uri.parse(UpiFixtures.URL))
        val apps = (1..appCount).map { index ->
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = "test.upi.bank$index"
                    name = "test.upi.PaymentActivity"
                    exported = true
                    applicationInfo = ApplicationInfo().apply { packageName = "test.upi.bank$index" }
                }
            }
        }
        shadowOf(application.packageManager).setResolveInfosForIntent(target, apps)
        val intent = UpiAppChooserContract().createIntent(application, args)
        ActivityScenario.launchActivityForResult<UpiAppChooserActivity>(intent).use(block)
    }
}
