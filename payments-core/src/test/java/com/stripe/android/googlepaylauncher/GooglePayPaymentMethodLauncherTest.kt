package com.stripe.android.googlepaylauncher

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.googlepaylauncher.utils.LauncherIntegrationType
import com.stripe.android.googlepaylauncher.utils.runGooglePayPaymentMethodLauncherTest
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
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
            val firedEvents = mutableListOf<String>()

            val launcher = GooglePayPaymentMethodLauncher(
                lifecycleScope = activity.lifecycleScope,
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
                cardBrandFilter = DefaultCardBrandFilter
            )
            launcher.present(currencyCode = "usd")

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

    private companion object {
        val CONFIG = GooglePayPaymentMethodLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
