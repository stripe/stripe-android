package com.stripe.android.googlepay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import org.junit.Ignore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeGooglePayActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val contract = StripeGooglePayContract()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Ignore("failing in CI but not locally")
    fun `successful start should return Unavailable result`() {
        createActivity(
            ARGS
        ) { activityScenario ->
            // Google Pay is only available on a real device
            assertThat(parseResult(activityScenario))
                .isInstanceOf(StripeGooglePayContract.Result.Unavailable::class.java)
        }
    }

    @Test
    fun `start without args should finish with Error result`() {
        ActivityScenario.launch<StripeGooglePayActivity>(
            Intent(context, StripeGooglePayActivity::class.java)
        ).use { activityScenario ->
            assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result = parseResult(activityScenario) as StripeGooglePayContract.Result.Error
            assertThat(result.exception.message)
                .isEqualTo(
                    "StripeGooglePayActivity was started without arguments."
                )
        }
    }

    @Test
    fun `should update statusBarColor`() {
        runOnActivityScenario(ARGS) { activityScenario ->
            activityScenario.onActivity { activity ->
                assertThat(activity.window.statusBarColor)
                    .isEqualTo(Color.RED)
                activity.finish()
            }
        }
    }

    private fun createActivity(
        args: StripeGooglePayContract.Args,
        onActivityScenario: (ActivityScenario<StripeGooglePayActivity>) -> Unit
    ) {
        runOnActivityScenario(args) { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.finish()
            }
            onActivityScenario(activityScenario)
        }
    }

    private fun runOnActivityScenario(
        args: StripeGooglePayContract.Args,
        onActivityScenario: (ActivityScenario<StripeGooglePayActivity>) -> Unit
    ) {
        ActivityScenario.launch<StripeGooglePayActivity>(
            contract.createIntent(
                context,
                args
            )
        ).use {
            onActivityScenario(it)
        }
    }

    private fun parseResult(
        activityScenario: ActivityScenario<*>
    ): StripeGooglePayContract.Result {
        return contract.parseResult(0, activityScenario.result.resultData)
    }

    private companion object {
        private val CONFIG = StripeGooglePayContract.GooglePayConfig(
            environment = StripeGooglePayEnvironment.Test,
            amount = 1000,
            countryCode = "US",
            currencyCode = "usd",
            isEmailRequired = true,
            transactionId = "pi_1F7J1aCRMbs6FrXfaJcvbxF6"
        )

        private val ARGS = StripeGooglePayContract.Args(
            config = CONFIG,
            statusBarColor = Color.RED
        )
    }
}
