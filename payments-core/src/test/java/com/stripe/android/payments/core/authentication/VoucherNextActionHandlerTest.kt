package com.stripe.android.payments.core.authentication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModelTestFakes.FakeAuthActivityStarterHost
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VoucherNextActionHandlerTest {
    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun `Hosted voucher delegates caller configuration to web authentication`() = runScenario {
        handler.performNextAction(host, PaymentIntentFixtures.OXXO_REQUIRES_ACTION, API_CONFIGURATION)

        assertThat(browserStarter.calls.awaitItem().apiConfiguration).isSameInstanceAs(API_CONFIGURATION)
        relayStarter.calls.expectNoEvents()
    }

    @Test
    fun `Missing hosted voucher delegates caller account to no-op authentication`() = runScenario {
        val intent = PaymentIntentFixtures.OXXO_REQUIRES_ACTION.copy(
            nextActionData = StripeIntent.NextActionData.DisplayOxxoDetails(
                expiresAfter = 0,
                number = null,
                hostedVoucherUrl = null,
            )
        )

        handler.performNextAction(host, intent, API_CONFIGURATION)

        assertThat(relayStarter.calls.awaitItem().toResult().stripeAccountId)
            .isEqualTo(API_CONFIGURATION.stripeAccountId)
        browserStarter.calls.expectNoEvents()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val browserStarter = FakeBrowserStarter()
        val relayStarter = FakeRelayStarter()
        val analyticsExecutor = FakeAnalyticsExecutor()
        val host = FakeAuthActivityStarterHost()
        val handler = VoucherNextActionHandler(
            webIntentAuthenticator = WebIntentNextActionHandler(
                paymentBrowserAuthStarterFactory = { browserStarter },
                analyticsRequestExecutor = analyticsExecutor,
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = context,
                    publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                ),
                enableLogging = false,
                uiContext = UnconfinedTestDispatcher(testScheduler),
                isInstantApp = false,
                defaultReturnUrl = DefaultReturnUrl.create(context),
                redirectResolver = RedirectResolver { error("Voucher authentication should not resolve redirects") },
            ),
            noOpIntentAuthenticator = NoOpIntentNextActionHandler { relayStarter },
            context = context,
        )

        Scenario(handler, host, browserStarter, relayStarter).block()

        browserStarter.calls.ensureAllEventsConsumed()
        relayStarter.calls.ensureAllEventsConsumed()
        analyticsExecutor.calls.ensureAllEventsConsumed()
        host.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val handler: VoucherNextActionHandler,
        val host: FakeAuthActivityStarterHost,
        val browserStarter: FakeBrowserStarter,
        val relayStarter: FakeRelayStarter,
    )

    internal class FakeBrowserStarter : PaymentBrowserAuthStarter {
        val calls = Turbine<PaymentBrowserAuthContract.Args>()

        override fun start(args: PaymentBrowserAuthContract.Args) {
            calls.add(args)
        }
    }

    internal class FakeRelayStarter : PaymentRelayStarter {
        val calls = Turbine<PaymentRelayStarter.Args>()

        override fun start(args: PaymentRelayStarter.Args) {
            calls.add(args)
        }
    }

    internal class FakeAnalyticsExecutor : AnalyticsRequestExecutor {
        val calls = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            calls.add(request)
        }
    }

    private companion object {
        val API_CONFIGURATION = ApiConfiguration.State(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
        )
    }
}
