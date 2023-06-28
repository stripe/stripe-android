package com.stripe.android.googlepaylauncher

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito
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
class GooglePayPaymentMethodLauncherTest {

    @get:Rule
    val intentsTestRule = IntentsTestRule(ComponentActivity::class.java)

    @Test
    fun `present() should successfully return a result when Google Pay is available`() {
        val result = GooglePayPaymentMethodLauncher.Result.Completed(CARD_PAYMENT_METHOD)

        val readyCallback = mock<GooglePayPaymentMethodLauncher.ReadyCallback>()
        val resultCallback = mock<GooglePayPaymentMethodLauncher.ResultCallback>()

        runGooglePayPaymentMethodLauncherTest(result) { scenario, activity ->
            val launcher = GooglePayPaymentMethodLauncher(
                activity = activity,
                config = CONFIG,
                readyCallback = readyCallback,
                resultCallback = resultCallback,
            )
            scenario.moveToState(Lifecycle.State.RESUMED)
            launcher.present(currencyCode = "usd")
        }

        verify(readyCallback).onReady(eq(true))
        verify(resultCallback).onResult(eq(result))
    }

    @Test
    fun `init should fire expected event`() {
        val firedEvents = mutableListOf<String>()
        val result = GooglePayPaymentMethodLauncher.Result.Completed(CARD_PAYMENT_METHOD)

        runGooglePayPaymentMethodLauncherTest(result) { scenario, activity ->
            val launcher = GooglePayPaymentMethodLauncher(
                lifecycleScope = activity.lifecycleScope,
                config = CONFIG,
                readyCallback = mock(),
                activityResultLauncher = mock(),
                skipReadyCheck = true,
                context = activity,
                googlePayRepositoryFactory = mock(),
                productUsage = emptySet(),
                publishableKeyProvider = { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
                stripeAccountIdProvider = { null },
                paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                    context = activity,
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ),
                analyticsRequestExecutor = { firedEvents += it.params["event"].toString() },
            )
            scenario.moveToState(Lifecycle.State.RESUMED)
            launcher.present(currencyCode = "usd")
        }

        assertThat(firedEvents).containsExactly("stripe_android.googlepaypaymentmethodlauncher_init")
    }

    @Test
    fun `present() should throw IllegalStateException when Google Pay is not available`() {
        val result = GooglePayPaymentMethodLauncher.Result.Completed(CARD_PAYMENT_METHOD)
        val readyCallback = mock<GooglePayPaymentMethodLauncher.ReadyCallback>()

        runGooglePayPaymentMethodLauncherTest(isReady = false, result = result) { _, activity ->
            val launcher = GooglePayPaymentMethodLauncher(
                activity = activity,
                config = CONFIG,
                readyCallback = readyCallback,
                resultCallback = mock(),
            )

            assertFailsWith<IllegalStateException> {
                launcher.present(currencyCode = "usd")
            }
        }

        verify(readyCallback).onReady(eq(false))
    }

    private fun runGooglePayPaymentMethodLauncherTest(
        result: GooglePayPaymentMethodLauncher.Result,
        isReady: Boolean = true,
        block: (ActivityScenario<ComponentActivity>, ComponentActivity) -> Unit,
    ) {
        Mockito.mockStatic(Wallet::class.java).use { wallet ->
            val paymentsClient = mock<PaymentsClient>()
            whenever(paymentsClient.isReadyToPay(any())).thenReturn(Tasks.forResult(isReady))

            // Provide a static mock here because DefaultGooglePayRepository calls this static
            // create method.
            wallet.`when`<PaymentsClient> {
                Wallet.getPaymentsClient(isA<Context>(), any())
            }.thenReturn(paymentsClient)

            // Mock the return value from GooglePayLauncherActivity so that we immediately return
            val resultData = Intent().putExtras(
                bundleOf(GooglePayPaymentMethodLauncherContractV2.EXTRA_RESULT to result)
            )
            val activityResult = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
            Intents.intending(IntentMatchers.anyIntent()).respondWith(activityResult)

            val scenario = ActivityScenario.launch(ComponentActivity::class.java)
            scenario.moveToState(Lifecycle.State.CREATED)

            scenario.onActivity { activity ->
                PaymentConfiguration.init(activity, "pk_test")
                block(scenario, activity)
            }

            Espresso.onIdle()
        }
    }

    internal class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = FrameLayout(inflater.context)
    }

    private companion object {
        val CONFIG = GooglePayPaymentMethodLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
