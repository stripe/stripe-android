package com.stripe.android.googlepaylauncher

import androidx.activity.ComponentActivity
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.utils.LauncherIntegrationType
import com.stripe.android.googlepaylauncher.utils.runGooglePayPaymentMethodLauncherTest
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.testing.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class GooglePayPaymentMethodLauncherTest {

    @get:Rule
    val intentsTestRule = IntentsTestRule(ComponentActivity::class.java)

    @Test
    fun `present() should successfully return a result when Google Pay is available`() {
        val result = GooglePayPaymentMethodLauncher.Result.Completed(CARD_PAYMENT_METHOD)

        runGooglePayPaymentMethodLauncherTest(
            result = result,
        ) { _, launcher ->
            launcher.present(currencyCode = "usd")
        }
    }

    @Test
    fun `init should fire expected event`() {
        runGooglePayPaymentMethodLauncherTest(
            integrationTypes = listOf(LauncherIntegrationType.Activity),
            expectResult = false,
        ) { activity, _ ->
            GooglePayPaymentMethodLauncher.HAS_SENT_INIT_ANALYTIC_EVENT = false

            val firedEvents = mutableListOf<String>()

            val launcher = GooglePayPaymentMethodLauncher(
                lifecycleOwner = activity,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                skipReadyCheck = true,
                context = activity,
                googlePayRepositoryFactory = mock(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
                cardBrandFilter = DefaultCardBrandFilter,
                cardFundingFilter = DefaultCardFundingFilter
            )
            launcher.present(currencyCode = "usd")

            assertThat(firedEvents).containsExactly("stripe_android.googlepaypaymentmethodlauncher_init")
        }
    }

    @Test
    fun `init should fire expected event only on first init`() {
        runGooglePayPaymentMethodLauncherTest(
            integrationTypes = listOf(LauncherIntegrationType.Activity),
            expectResult = false,
        ) { activity, _ ->
            GooglePayPaymentMethodLauncher.HAS_SENT_INIT_ANALYTIC_EVENT = false

            val firedEvents = mutableListOf<String>()

            GooglePayPaymentMethodLauncher(
                lifecycleOwner = activity,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                skipReadyCheck = true,
                context = activity,
                googlePayRepositoryFactory = mock(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
                cardBrandFilter = DefaultCardBrandFilter,
                cardFundingFilter = DefaultCardFundingFilter
            )

            GooglePayPaymentMethodLauncher(
                lifecycleOwner = activity,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                skipReadyCheck = true,
                context = activity,
                googlePayRepositoryFactory = mock(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
                cardBrandFilter = DefaultCardBrandFilter,
                cardFundingFilter = DefaultCardFundingFilter
            )

            assertThat(firedEvents).containsExactly("stripe_android.googlepaypaymentmethodlauncher_init")
        }
    }

    @Test
    fun `present() should throw IllegalStateException when Google Pay is not available`() {
        runGooglePayPaymentMethodLauncherTest(
            isReady = false,
            expectResult = false,
        ) { _, launcher ->
            assertFailsWith<IllegalStateException> {
                launcher.present(currencyCode = "usd")
            }
        }
    }

    @Test
    fun `present() uses explicit publishable key and preserves global stripe account`() = runScenario {
        launcher.present(
            currencyCode = "usd",
            clientAttributionMetadata = null,
            publishableKey = "pk_passed_in_args",
        )

        val args = activityResultCaller.awaitLaunchCall() as GooglePayPaymentMethodLauncherContractV2.Args
        assertThat(args.apiConfiguration.publishableKey).isEqualTo("pk_passed_in_args")
        assertThat(args.apiConfiguration.stripeAccountId).isEqualTo(ApiKeyFixtures.FAKE_STRIPE_ACCOUNT)
    }

    @Test
    fun `present() uses global configuration when publishable key is not provided`() = runScenario {
        launcher.present(currencyCode = "usd")

        val args = activityResultCaller.awaitLaunchCall() as GooglePayPaymentMethodLauncherContractV2.Args
        assertThat(args.apiConfiguration.publishableKey).isEqualTo(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        assertThat(args.apiConfiguration.stripeAccountId).isEqualTo(ApiKeyFixtures.FAKE_STRIPE_ACCOUNT)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val activity = intentsTestRule.activity
        PaymentConfiguration.init(
            context = activity,
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
        )

        DummyActivityResultCaller.test {
            val activityResultLauncher = activityResultCaller.registerForActivityResult(
                GooglePayPaymentMethodLauncherContractV2()
            ) { error("No result expected") }
            awaitRegisterCall()
            awaitNextRegisteredLauncher()

            val launcher = GooglePayPaymentMethodLauncher(
                lifecycleOwner = activity,
                config = CONFIG,
                readyCallback = { error("Readiness check should be skipped") },
                activityResultLauncher = activityResultLauncher,
                skipReadyCheck = true,
                context = activity,
                googlePayRepositoryFactory = mock(),
                cardBrandFilter = DefaultCardBrandFilter,
                cardFundingFilter = DefaultCardFundingFilter,
                analyticsRequestExecutor = {},
            )

            Scenario(launcher, this).block()
        }
    }

    private data class Scenario(
        val launcher: GooglePayPaymentMethodLauncher,
        val activityResultCaller: DummyActivityResultCaller.Scenario,
    )

    private companion object {
        val CONFIG = GooglePayPaymentMethodLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
