package com.stripe.android.payments.paymentlauncher

import android.graphics.Color
import android.os.Parcel
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.exception.GenericStripeException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@OptIn(SharedPaymentTokenSessionPreview::class)
class StripePaymentLauncherTest {
    @Test
    fun `confirm payment forwards params and API configuration`() = runScenario {
        val params = ConfirmPaymentIntentParams.create(CLIENT_SECRET)

        paymentLauncher.confirm(params)

        val args = awaitLaunch() as PaymentLauncherContract.Args.IntentConfirmationArgs
        assertThat(args.confirmStripeIntentParams).isEqualTo(params)
        assertForwardedConfiguration(args)
    }

    @Test
    fun `confirm setup forwards params and API configuration`() = runScenario {
        val params = ConfirmSetupIntentParams.createWithoutPaymentMethod(CLIENT_SECRET)

        paymentLauncher.confirm(params)

        val args = awaitLaunch() as PaymentLauncherContract.Args.IntentConfirmationArgs
        assertThat(args.confirmStripeIntentParams).isEqualTo(params)
        assertForwardedConfiguration(args)
    }

    @Test
    fun `handle next action for payment forwards client secret and API configuration`() = runScenario {
        paymentLauncher.handleNextActionForPaymentIntent(CLIENT_SECRET)

        val args = awaitLaunch() as PaymentLauncherContract.Args.PaymentIntentNextActionArgs
        assertThat(args.paymentIntentClientSecret).isEqualTo(CLIENT_SECRET)
        assertForwardedConfiguration(args)
    }

    @Test
    fun `handle next action for setup forwards client secret and API configuration`() = runScenario {
        paymentLauncher.handleNextActionForSetupIntent(CLIENT_SECRET)

        val args = awaitLaunch() as PaymentLauncherContract.Args.SetupIntentNextActionArgs
        assertThat(args.setupIntentClientSecret).isEqualTo(CLIENT_SECRET)
        assertForwardedConfiguration(args)
    }

    @Test
    fun `handle next action with intent forwards intent and API configuration`() = runScenario {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        paymentLauncher.handleNextActionForStripeIntent(intent)

        val args = awaitLaunch() as PaymentLauncherContract.Args.StripeIntentNextActionWithIntentArgs
        assertThat(args.stripeIntent).isEqualTo(intent)
        assertForwardedConfiguration(args)
    }

    @Test
    fun `launch preserves null Stripe account through parceling`() = runScenario(
        apiConfiguration = ApiConfiguration(PUBLISHABLE_KEY).build(),
    ) {
        paymentLauncher.handleNextActionForPaymentIntent(CLIENT_SECRET)

        val args = awaitLaunch()
        assertThat(args.apiConfiguration.stripeAccountId).isNull()
        assertForwardedConfiguration(args)
    }

    @Test
    fun `each launch reads the current API configuration once`() = runScenario {
        paymentLauncher.handleNextActionForPaymentIntent(CLIENT_SECRET)
        val firstArgs = awaitLaunch()
        assertForwardedConfiguration(firstArgs)

        val updatedConfiguration = ApiConfiguration("pk_test_updated")
            .stripeAccountId("acct_updated")
            .build()
        apiConfigurationProvider.value = updatedConfiguration
        paymentLauncher.handleNextActionForPaymentIntent(CLIENT_SECRET)

        val secondArgs = awaitLaunch()
        assertForwardedConfiguration(secondArgs)
        assertThat(firstArgs.apiConfiguration).isSameInstanceAs(API_CONFIGURATION)
        assertThat(secondArgs.apiConfiguration).isSameInstanceAs(updatedConfiguration)
    }

    @Test
    fun `hashed payment uses decoded key and launcher account through parceling`() = runScenario {
        val decodedKey = "pk_test_from_hashed_value"
        paymentLauncher.handleNextActionForHashedPaymentIntent(
            Base64.encodeToString("$decodedKey:$CLIENT_SECRET".toByteArray(), Base64.NO_WRAP)
        )

        val args = awaitLaunch() as PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs
        assertThat(args.apiConfiguration.publishableKey).isEqualTo(decodedKey)
        assertThat(args.apiConfiguration.stripeAccountId).isEqualTo(STRIPE_ACCOUNT_ID)
        assertThat(args.paymentIntentClientSecret).isEqualTo(CLIENT_SECRET)
        assertThat(args.validate().isSuccess).isTrue()

        val restoredArgs = roundTrip(args) as PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs
        assertThat(restoredArgs.apiConfiguration).isEqualTo(args.apiConfiguration)
        assertThat(restoredArgs.paymentIntentClientSecret).isEqualTo(CLIENT_SECRET)
        assertThat(restoredArgs.validate().isSuccess).isTrue()
    }

    @Test
    fun `invalid Base64 preserves validation error and unknown credentials through parceling`() =
        assertInvalidHash(
            hashedValue = "random_value===",
            analyticsValue = "invalidHashedValueNotBase64",
        )

    @Test
    fun `invalid decoded format preserves validation error and unknown credentials through parceling`() =
        assertInvalidHash(
            hashedValue = Base64.encodeToString("missing_separator".toByteArray(), Base64.NO_WRAP),
            analyticsValue = "invalidHashedValueIncorrectFormat",
        )

    private fun assertInvalidHash(hashedValue: String, analyticsValue: String) = runScenario {
        paymentLauncher.handleNextActionForHashedPaymentIntent(hashedValue)

        val args = awaitLaunch() as PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs
        val originalError = args.validate().exceptionOrNull() as GenericStripeException
        assertThat(originalError.analyticsValue()).isEqualTo(analyticsValue)
        assertThat(args.apiConfiguration.publishableKey).isEqualTo("UNKNOWN")

        val restoredArgs = roundTrip(args) as PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs
        val restoredError = restoredArgs.validate().exceptionOrNull() as GenericStripeException
        assertThat(restoredError.analyticsValue()).isEqualTo(analyticsValue)
        assertThat(restoredArgs.apiConfiguration.publishableKey).isEqualTo("UNKNOWN")
        assertThat(restoredArgs.apiConfiguration.stripeAccountId).isEqualTo(STRIPE_ACCOUNT_ID)
        assertThat(restoredArgs.paymentIntentClientSecret).isEqualTo("UNKNOWN")
    }

    private fun runScenario(
        apiConfiguration: ApiConfiguration.State = API_CONFIGURATION,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val hostActivityLauncher = FakeLauncher()
        val apiConfigurationProvider = FakeApiConfigurationProvider(apiConfiguration)
        val paymentLauncher = StripePaymentLauncher(
            apiConfigurationProvider = apiConfigurationProvider,
            hostActivityLauncher = hostActivityLauncher,
            enableLogging = false,
            productUsage = setOf("PaymentLauncher"),
            includePaymentSheetNextHandlers = false,
            statusBarColor = Color.RED,
        )

        Scenario(paymentLauncher, hostActivityLauncher, apiConfigurationProvider).block()
        hostActivityLauncher.launchCalls.ensureAllEventsConsumed()
        hostActivityLauncher.unregisterCalls.ensureAllEventsConsumed()
        apiConfigurationProvider.getCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val paymentLauncher: StripePaymentLauncher,
        val hostActivityLauncher: FakeLauncher,
        val apiConfigurationProvider: FakeApiConfigurationProvider,
    ) {
        suspend fun awaitLaunch(): PaymentLauncherContract.Args {
            assertThat(apiConfigurationProvider.getCalls.awaitItem()).isSameInstanceAs(apiConfigurationProvider.value)
            return hostActivityLauncher.launchCalls.awaitItem()
        }

        fun assertForwardedConfiguration(args: PaymentLauncherContract.Args) {
            assertThat(args.apiConfiguration).isSameInstanceAs(apiConfigurationProvider.value)
            assertThat(roundTrip(args).apiConfiguration).isEqualTo(apiConfigurationProvider.value)
        }
    }

    internal class FakeLauncher : ActivityResultLauncher<PaymentLauncherContract.Args>() {
        override val contract = PaymentLauncherContract()
        val launchCalls = Turbine<PaymentLauncherContract.Args>()
        val unregisterCalls = Turbine<Unit>()

        override fun launch(input: PaymentLauncherContract.Args, options: ActivityOptionsCompat?) {
            launchCalls.add(input)
        }

        override fun unregister() {
            unregisterCalls.add(Unit)
        }
    }

    internal class FakeApiConfigurationProvider(
        var value: ApiConfiguration.State,
    ) : Provider<ApiConfiguration.State> {
        val getCalls = Turbine<ApiConfiguration.State>()

        override fun get(): ApiConfiguration.State {
            getCalls.add(value)
            return value
        }
    }

    private companion object {
        const val PUBLISHABLE_KEY = "pk_test_launcher"
        const val STRIPE_ACCOUNT_ID = "acct_launcher"
        const val CLIENT_SECRET = "clientSecret"
        val API_CONFIGURATION = ApiConfiguration(PUBLISHABLE_KEY)
            .stripeAccountId(STRIPE_ACCOUNT_ID)
            .build()

        @Suppress("DEPRECATION")
        fun roundTrip(args: PaymentLauncherContract.Args): PaymentLauncherContract.Args {
            val parcel = Parcel.obtain()
            return try {
                parcel.writeParcelable(args, 0)
                parcel.setDataPosition(0)
                requireNotNull(parcel.readParcelable(PaymentLauncherContract.Args::class.java.classLoader))
            } finally {
                parcel.recycle()
            }
        }
    }
}
