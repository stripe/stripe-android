package com.stripe.android.googlepaylauncher

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class GooglePayLauncherTest {

    @get:Rule
    val intentsTestRule = IntentsTestRule(ComponentActivity::class.java)

    @Test
    fun `presentForPaymentIntent() should successfully return a result when Google Pay is available`() {
        val readyCallback = mock<GooglePayLauncher.ReadyCallback>()
        val resultCallback = mock<GooglePayLauncher.ResultCallback>()

        runGooglePayLauncherTest { scenario, activity ->
            val launcher = GooglePayLauncher(
                activity = activity,
                config = CONFIG,
                readyCallback = readyCallback,
                resultCallback = resultCallback,
            )
            scenario.moveToState(Lifecycle.State.RESUMED)
            launcher.presentForPaymentIntent("pi_123_secret_456")
        }

        verify(readyCallback).onReady(eq(true))
        verify(resultCallback).onResult(eq(GooglePayLauncher.Result.Completed))
    }

    @Test
    fun `init should fire expected event`() {
        val firedEvents = mutableListOf<String>()

        runGooglePayLauncherTest { _, activity ->
            // Have to use the internal constructor here to provide a mock request executor
            // ¯\_(ツ)_/¯
            GooglePayLauncher(
                lifecycleScope = activity.lifecycleScope,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                googlePayRepositoryFactory = mock(),
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
            )
        }

        assertThat(firedEvents).containsExactly("stripe_android.googlepaylauncher_init")
    }

    @Test
    fun `presentForPaymentIntent() should throw IllegalStateException when Google Pay is not available`() {
        val readyCallback = mock<GooglePayLauncher.ReadyCallback>()
        val resultCallback = mock<GooglePayLauncher.ResultCallback>()

        runGooglePayLauncherTest(isReady = false) { scenario, activity ->
            val launcher = GooglePayLauncher(
                activity = activity,
                config = CONFIG,
                readyCallback = readyCallback,
                resultCallback = resultCallback,
            )

            scenario.moveToState(Lifecycle.State.RESUMED)

            assertFailsWith<IllegalStateException> {
                launcher.presentForPaymentIntent("pi_123")
            }
        }

        verify(readyCallback).onReady(eq(false))
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

    private fun runGooglePayLauncherTest(
        isReady: Boolean = true,
        result: GooglePayLauncher.Result = GooglePayLauncher.Result.Completed,
        block: (ActivityScenario<ComponentActivity>, ComponentActivity) -> Unit,
    ) {
        mockStatic(Wallet::class.java).use { wallet ->
            val paymentsClient = mock<PaymentsClient>()
            whenever(paymentsClient.isReadyToPay(any())).thenReturn(Tasks.forResult(isReady))

            // Provide a static mock here because DefaultGooglePayRepository calls this static
            // create method.
            wallet.`when`<PaymentsClient> {
                Wallet.getPaymentsClient(isA<Context>(), any())
            }.thenReturn(paymentsClient)

            // Mock the return value from GooglePayLauncherActivity so that we immediately return
            val resultData = Intent().putExtras(
                bundleOf(GooglePayLauncherContract.EXTRA_RESULT to result)
            )
            val activityResult = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
            intending(anyIntent()).respondWith(activityResult)

            val scenario = ActivityScenario.launch(ComponentActivity::class.java)
            scenario.moveToState(Lifecycle.State.CREATED)

            scenario.onActivity { activity ->
                PaymentConfiguration.init(activity, "pk_test")
                block(scenario, activity)
            }

            Espresso.onIdle()
        }
    }

    private companion object {
        val CONFIG = GooglePayLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
