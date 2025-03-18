package com.stripe.android.paymentelement.confirmation.lpms

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.getIntents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.paymentelement.confirmation.lpms.foundations.CreateIntentFactory
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmNetworkTestActivity
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.PublishableKeyFetcher
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.RetryRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.RuleChain
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

internal open class BaseLpmNetworkTest(
    private val paymentMethodType: PaymentMethod.Type,
) {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val rules: RuleChain = RuleChain.emptyRuleChain()
        .around(createTestActivityRule<LpmNetworkTestActivity>())
        .around(IntentsRule())
        .around(PaymentElementCallbackTestRule())
        .around(RetryRule(attempts = 3))

    fun test(
        testType: TestType,
        country: MerchantCountry = MerchantCountry.US,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingDetails: AddressDetails? = null,
        customerRequestedSave: Boolean = false,
        allowsManualConfirmation: Boolean = false,
    ) = runLpmNetworkTest(country = country, allowsManualConfirmation = allowsManualConfirmation) {
        val factory = CreateIntentFactory(
            paymentElementCallbackIdentifier = LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
            paymentMethodType = paymentMethodType,
            testClient = testClient,
        )

        val createIntentData = testType.createIntent(country, factory)

        createIntentData.onSuccess { data ->
            assertCanConfirmIntent(
                intent = data.intent,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
                initializationMode = data.initializationMode,
            )
        }.onFailure { exception ->
            fail(exception.message, exception)
        }
    }

    private fun runLpmNetworkTest(
        country: MerchantCountry,
        allowsManualConfirmation: Boolean,
        test: suspend LpmNetworkTestActivity.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        val result = PublishableKeyFetcher.publishableKey(country)

        assertThat(result.isSuccess).isTrue()

        val publishableKey = result.getOrThrow()

        ActivityScenario.launch<LpmNetworkTestActivity>(
            LpmNetworkTestActivity.createIntent(
                context = application,
                args = LpmNetworkTestActivity.Args(
                    publishableKey = publishableKey,
                    paymentElementCallbackIdentifier = LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
                    allowsManualConfirmation = allowsManualConfirmation,
                ),
            )
        ).use { scenario ->
            val testStarted = CountDownLatch(1)
            lateinit var job: Job

            scenario.onActivity { activity ->
                job = launch {
                    test(activity)
                }
                testStarted.countDown()
            }

            testStarted.await(5, TimeUnit.SECONDS)

            withTimeout(90.seconds) {
                job.join()
            }
        }
    }

    private suspend fun LpmNetworkTestActivity.assertCanConfirmIntent(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
        shippingDetails: AddressDetails?,
        customerRequestedSave: Boolean,
    ) {
        intendingPaymentConfirmationToBeLaunched(intent)

        val option = PaymentMethodConfirmationOption.New(
            createParams = createParams,
            optionsParams = optionsParams,
            extraParams = extraParams,
            shouldSave = customerRequestedSave,
        )

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = option,
                    intent = intent,
                    initializationMode = initializationMode,
                    appearance = PaymentSheet.Appearance.Builder().build(),
                    shippingDetails = shippingDetails,
                )
            )

            val confirming = awaitItem().assertConfirming()

            assertThat(confirming.option).isEqualTo(option)

            intendedPaymentConfirmationToBeLaunched()

            val complete = awaitItem().assertComplete()

            val successResult = complete.result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(intent)

            ensureAllEventsConsumed()
        }

        val recordedIntent = getIntents().first()

        recordedIntent.assertPaymentLauncherName()

        val arguments = recordedIntent.assertPaymentLauncherArgs()

        assertConfirmed(arguments.confirmStripeIntentParams)
    }

    private fun intendingPaymentConfirmationToBeLaunched(
        intent: StripeIntent,
    ) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(bundleOf("extra_args" to InternalPaymentResult.Completed(intent)))
            )
        )
    }

    private fun intendedPaymentConfirmationToBeLaunched() {
        intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
    }

    private fun Intent.assertPaymentLauncherName() {
        assertThat(component?.className).isEqualTo(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)
    }

    private fun Intent.assertPaymentLauncherArgs(): PaymentLauncherContract.Args.IntentConfirmationArgs {
        @Suppress("DEPRECATION")
        val arguments = extras?.getParcelable<PaymentLauncherContract.Args.IntentConfirmationArgs>("extra_args")

        assertThat(arguments).isNotNull()

        return requireNotNull(arguments)
    }

    private suspend fun LpmNetworkTestActivity.assertConfirmed(
        confirmParams: ConfirmStripeIntentParams
    ) {
        val result = when (confirmParams) {
            is ConfirmSetupIntentParams -> {
                testClient.confirmSetupIntent(
                    confirmParams = confirmParams,
                )
            }
            is ConfirmPaymentIntentParams -> {
                testClient.confirmPaymentIntent(
                    confirmParams = confirmParams,
                )
            }
        }

        result.onSuccess { intent ->
            /*
             * Our intent should either be confirmed state or in a state where an action is required. In either case,
             * this indicates to us that the Stripe API accept the provided intent confirmation parameters and
             * therefore can successfully confirm using the provided parameters
             */
            if (!intent.isConfirmed && !intent.requiresAction()) {
                fail(
                    intent.lastErrorMessage
                        ?: "Intent was neither confirmed nor requires next action due to unknown error!"
                )
            }
        }.onFailure { failure ->
            fail(failure.message, failure)
        }
    }

    private companion object {
        const val LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER = "LpmNetworkTestIdentifier"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
