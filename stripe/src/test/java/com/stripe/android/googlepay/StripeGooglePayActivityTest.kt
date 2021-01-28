package com.stripe.android.googlepay

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
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
            PAYMENT_DATA_ARGS
        ) { activityScenario ->
            // Google Pay is only available on a real device
            assertThat(parseResult(activityScenario))
                .isInstanceOf(StripeGooglePayContract.Result.Unavailable::class.java)
        }
    }

    @Test
    fun `start should ConfirmPaymentIntent args and manual confirmation should finish with Error result`() {
        createActivity(
            CONFIRM_ARGS.copy(
                paymentIntent = PAYMENT_INTENT.copy(
                    confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
                )
            )
        ) { activityScenario ->
            val result = parseResult(activityScenario) as StripeGooglePayContract.Result.Error
            assertThat(result.exception.message)
                .isEqualTo(
                    "StripeGooglePayActivity requires a PaymentIntent with automatic confirmation."
                )
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

    private fun createActivity(
        args: StripeGooglePayContract.Args,
        onCreated: (ActivityScenario<StripeGooglePayActivity>) -> Unit
    ) {
        ActivityScenario.launch<StripeGooglePayActivity>(
            contract.createIntent(
                context,
                args
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                activity.finish()
            }
            onCreated(activityScenario)
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
            countryCode = "US",
            isEmailRequired = true
        )

        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
            confirmationMethod = PaymentIntent.ConfirmationMethod.Automatic
        )

        private val PAYMENT_DATA_ARGS = StripeGooglePayContract.Args.PaymentData(
            paymentIntent = PAYMENT_INTENT,
            config = CONFIG
        )

        private val CONFIRM_ARGS = StripeGooglePayContract.Args.ConfirmPaymentIntent(
            paymentIntent = PAYMENT_INTENT,
            config = CONFIG
        )
    }
}
