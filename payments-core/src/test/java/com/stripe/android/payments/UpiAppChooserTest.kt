package com.stripe.android.payments

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.StripeIntentResult
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.browser.BrowserCapabilities
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class UpiAppChooserTest {
    @Test
    fun `launches an unrestricted chooser even when a Custom Tabs browser is installed`() = runScenario {
        val chooser = viewModel.createLaunchIntent(ARGS)
        val target = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))

        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        assertThat(chooser.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo("Pay with")
        assertThat(target.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(target.data.toString()).isEqualTo(UPI_URL)
        assertThat(target.`package`).isNull()
        assertThat(target.component).isNull()
        assertThat(target.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(0)
        assertThat(resolver.calls.takeItem().data).isEqualTo(Uri.parse(UPI_URL))
    }

    @Test
    fun `no compatible apps returns an actionable failure`() = runScenario(available = false) {
        val error = assertFailsWith<ActivityNotFoundException> { viewModel.createLaunchIntent(ARGS) }
        val result = PaymentFlowResult.Unvalidated.fromIntent(viewModel.getFailureIntent(ARGS, error))

        assertThat(resolver.calls.takeItem().action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(result.exception?.message).isEqualTo("No compatible UPI app")
        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.stripeAccountId).isEqualTo("acct_123")
    }

    @Test
    fun `missing mobile auth URL fails before querying apps`() = runScenario {
        val args = ARGS.copy(url = "")
        val error = assertFailsWith<IllegalArgumentException> { viewModel.createLaunchIntent(args) }
        val result = PaymentFlowResult.Unvalidated.fromIntent(viewModel.getFailureIntent(args, error))

        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.FAILED)
        assertThat(result.exception?.message).isEqualTo("Unable to authenticate")
    }

    @Test
    fun `an HTTPS URL cannot be launched as a UPI payment`() = runScenario {
        assertFailsWith<IllegalArgumentException> {
            viewModel.createLaunchIntent(ARGS.copy(url = "https://payments.stripe.com/upi/instructions/test"))
        }
    }

    @Test
    fun `an unrelated UPI action cannot be launched as a payment`() = runScenario {
        assertFailsWith<IllegalArgumentException> {
            viewModel.createLaunchIntent(ARGS.copy(url = "upi://collect?pa=merchant@upi"))
        }
    }

    @Test
    fun `return carries unknown outcome for API verification and cannot cancel the source`() = runScenario {
        val result = PaymentFlowResult.Unvalidated.fromIntent(viewModel.getResultIntent(ARGS))

        assertThat(result.flowOutcome).isEqualTo(StripeIntentResult.Outcome.UNKNOWN)
        assertThat(result.clientSecret).isEqualTo(ARGS.clientSecret)
        assertThat(result.stripeAccountId).isEqualTo("acct_123")
        assertThat(result.canCancelSource).isFalse()
    }

    private fun runScenario(available: Boolean = true, block: Scenario.() -> Unit) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val resolver = FakeActivityResolver(available)
        val analytics = FakeAnalyticsRequestExecutor()
        val viewModel = StripeBrowserLauncherViewModel(
            analyticsRequestExecutor = analytics,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = application,
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
            ),
            browserCapabilities = BrowserCapabilities.CustomTabs,
            customTabsPackage = "com.android.chrome",
            resolveErrorMessage = "Unable to authenticate",
            savedStateHandle = SavedStateHandle(),
            canResolveActivity = resolver::resolve,
            appChooserTitle = "Pay with",
            noCompatibleAppMessage = "No compatible UPI app",
        )
        Scenario(viewModel, resolver).block()
        resolver.calls.ensureAllEventsConsumed()
        analytics.calls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val viewModel: StripeBrowserLauncherViewModel,
        val resolver: FakeActivityResolver,
    )

    private class FakeActivityResolver(private val available: Boolean) {
        val calls = Turbine<Intent>()

        fun resolve(intent: Intent): Boolean {
            calls.add(intent)
            return available
        }
    }

    private class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val calls = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            calls.add(request)
        }
    }

    private companion object {
        const val UPI_URL = "upi://pay?pa=merchant%40upi&pn=Merchant%20Name&am=100.00&cu=INR&tr=123"
        val ARGS = PaymentBrowserAuthContract.Args(
            objectId = "pi_123",
            requestCode = 50000,
            clientSecret = "pi_123_secret_456",
            url = UPI_URL,
            apiConfiguration = ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_123"),
            statusBarColor = null,
            isInstantApp = false,
            shouldCancelIntentOnUserNavigation = false,
            shouldUseAppChooser = true,
        )
    }
}
