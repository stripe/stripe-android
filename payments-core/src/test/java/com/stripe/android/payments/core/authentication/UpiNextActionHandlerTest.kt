package com.stripe.android.payments.core.authentication

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpiNextActionHandlerTest {
    @Test
    fun `UPI selects app chooser and preserves Connect account and URL`() = runScenario {
        handler.performNextAction(
            host,
            PaymentIntentFixtures.PI_SUCCEEDED.copy(nextActionData = StripeIntent.NextActionData.UpiRedirect(URL)),
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_upi"),
        )

        val args = starter.calls.awaitItem()
        assertThat(args.shouldUseAppChooser).isTrue()
        assertThat(args.forceInAppWebView).isFalse()
        assertThat(args.shouldCancelSource).isFalse()
        assertThat(args.shouldCancelIntentOnUserNavigation).isFalse()
        assertThat(args.url).isEqualTo(URL)
        assertThat(args.apiConfiguration.stripeAccountId).isEqualTo("acct_upi")
        assertThat(args.returnUrl).isEqualTo(DefaultReturnUrl("test_package").value)
    }

    @Test
    fun `missing URL reaches launcher validation without crashing the next action handler`() = runScenario {
        handler.performNextAction(
            host,
            PaymentIntentFixtures.PI_SUCCEEDED.copy(nextActionData = StripeIntent.NextActionData.UpiRedirect(null)),
            ApiRequest.Options(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
        )

        val args = starter.calls.awaitItem()
        assertThat(args.url).isEmpty()
        assertThat(args.shouldUseAppChooser).isTrue()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val host = FakeHost()
        val starter = FakeBrowserAuthStarter()
        val analytics = FakeAnalyticsRequestExecutor()
        val handler = WebIntentNextActionHandler(
            paymentBrowserAuthStarterFactory = { starter },
            analyticsRequestExecutor = analytics,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                host.application,
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
            ),
            enableLogging = false,
            uiContext = UnconfinedTestDispatcher(testScheduler),
            apiConfigProvider = { ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, null) },
            isInstantApp = false,
            defaultReturnUrl = DefaultReturnUrl("test_package"),
            redirectResolver = RedirectResolver { error("UPI must launch the supplied URI directly") },
        )
        Scenario(handler, host, starter).block()
        starter.calls.ensureAllEventsConsumed()
        host.calls.ensureAllEventsConsumed()
        analytics.calls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val handler: WebIntentNextActionHandler,
        val host: FakeHost,
        val starter: FakeBrowserAuthStarter,
    )

    private class FakeBrowserAuthStarter : PaymentBrowserAuthStarter {
        val calls = Turbine<PaymentBrowserAuthContract.Args>()

        override fun start(args: PaymentBrowserAuthContract.Args) {
            calls.add(args)
        }
    }

    private class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val calls = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            calls.add(request)
        }
    }

    private class FakeHost : AuthActivityStarterHost {
        override val application: Application = ApplicationProvider.getApplicationContext()
        override val statusBarColor: Int? = null
        override val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        val calls = Turbine<Call>()

        override fun startActivityForResult(target: Class<*>, extras: Bundle, requestCode: Int) {
            calls.add(Call(target, extras, requestCode))
        }

        data class Call(val target: Class<*>, val extras: Bundle, val requestCode: Int)
    }

    private companion object {
        const val URL = "upi://pay?pa=merchant%40upi&am=100.00&cu=INR"
    }
}
