package com.stripe.android.googlepaylauncher

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.googlepaylauncher.utils.LauncherIntegrationType
import com.stripe.android.googlepaylauncher.utils.runGooglePayLauncherTest
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class GooglePayLauncherTest {

    @get:Rule
    val intentsTestRule = IntentsTestRule(ComponentActivity::class.java)

    @Test
    fun `presentForPaymentIntent() should successfully return a result when Google Pay is available`() {
        runGooglePayLauncherTest { _, launcher ->
            launcher.presentForPaymentIntent("pi_123_secret_456")
        }
    }

    @Test
    fun `init should fire expected event`() {
        runGooglePayLauncherTest(
            integrationTypes = listOf(LauncherIntegrationType.Activity),
            expectResult = false,
        ) { activity, _ ->
            GooglePayLauncher.HAS_SENT_INIT_ANALYTIC_EVENT = false
            val firedEvents = mutableListOf<String>()

            // Have to use the internal constructor here to provide a mock request executor
            // ¯\_(ツ)_/¯
            GooglePayLauncher(
                lifecycleScope = activity.lifecycleScope,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                googlePayRepositoryFactory = {
                    FakeGooglePayRepository(value = true)
                },
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
            )

            assertThat(firedEvents).containsExactly("stripe_android.googlepaylauncher_init")
        }
    }

    @Test
    fun `init should fire expected event on init`() {
        runGooglePayLauncherTest(
            integrationTypes = listOf(LauncherIntegrationType.Activity),
            expectResult = false,
        ) { activity, _ ->
            GooglePayLauncher.HAS_SENT_INIT_ANALYTIC_EVENT = false
            val firedEvents = mutableListOf<String>()

            // Have to use the internal constructor here to provide a mock request executor
            // ¯\_(ツ)_/¯
            GooglePayLauncher(
                lifecycleScope = activity.lifecycleScope,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                googlePayRepositoryFactory = {
                    FakeGooglePayRepository(value = true)
                },
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
            )

            GooglePayLauncher(
                lifecycleScope = activity.lifecycleScope,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                googlePayRepositoryFactory = {
                    FakeGooglePayRepository(value = true)
                },
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
            )

            assertThat(firedEvents).containsExactly("stripe_android.googlepaylauncher_init")
        }
    }

    @Test
    fun `presentForPaymentIntent() should throw IllegalStateException when Google Pay is not available`() {
        runGooglePayLauncherTest(
            isReady = false,
            expectResult = false,
        ) { _, launcher ->
            assertFailsWith<IllegalStateException> {
                launcher.presentForPaymentIntent("pi_123")
            }
        }
    }

    @Test
    fun `isJcbEnabled should return expected value for different merchantCountryCode`() {
        assertThat(
            CONFIG.copy(merchantCountryCode = "US").isJcbEnabled
        ).isFalse()

        assertThat(
            CONFIG.copy(merchantCountryCode = "JP").isJcbEnabled
        ).isTrue()

        assertThat(
            CONFIG.copy(merchantCountryCode = "jp").isJcbEnabled
        ).isTrue()
    }

    private companion object {
        val CONFIG = GooglePayLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
