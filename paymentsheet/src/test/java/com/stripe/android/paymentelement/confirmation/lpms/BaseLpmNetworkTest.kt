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
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmNetworkTestActivity
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.PublishableKeyFetcher
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.RetryRule
import com.stripe.android.testing.SetupIntentFactory
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
        amount: Int = 5050,
        currency: String = "USD",
        country: MerchantCountry = MerchantCountry.US,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingDetails: AddressDetails? = null,
        customerRequestedSave: Boolean = false,
        allowsManualConfirmation: Boolean = false,
    ) {
        when (testType) {
            TestType.PaymentIntent -> testWithPaymentIntent(
                amount = amount,
                currency = currency,
                createWithSetupFutureUsage = false,
                country = country,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
            )
            TestType.PaymentIntentWithSetupFutureUsage -> testWithPaymentIntent(
                amount = amount,
                currency = currency,
                createWithSetupFutureUsage = true,
                country = country,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
            )
            TestType.SetupIntent -> testWithSetupIntent(
                country = country,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
            )
            TestType.DeferredPaymentIntent -> testWithDeferredPaymentIntent(
                amount = amount,
                currency = currency,
                country = country,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                createWithSetupFutureUsage = false,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
                allowsManualConfirmation = allowsManualConfirmation,
            )
            TestType.DeferredPaymentIntentWithSetupFutureUsage -> testWithDeferredPaymentIntent(
                amount = amount,
                currency = currency,
                country = country,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                createWithSetupFutureUsage = true,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
                allowsManualConfirmation = allowsManualConfirmation,
            )
            TestType.DeferredSetupIntent -> testWithDeferredSetupIntent(
                country = country,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
                allowsManualConfirmation = allowsManualConfirmation,
            )
        }
    }

    private fun testWithPaymentIntent(
        amount: Int,
        currency: String,
        createWithSetupFutureUsage: Boolean,
        country: MerchantCountry,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
        shippingDetails: AddressDetails?,
        customerRequestedSave: Boolean,
    ) = runLpmNetworkTest(country = country, allowsManualConfirmation = false) {
        val fetchIntentResult = testClient.createPaymentIntent(
            country = country,
            amount = amount,
            currency = currency,
            paymentMethodType = paymentMethodType,
            createWithSetupFutureUsage = createWithSetupFutureUsage,
        ).mapCatching { clientSecret ->
            FetchIntentResult(
                clientSecret = clientSecret,
                intent = testClient.retrievePaymentIntent(clientSecret).getOrThrow(),
            )
        }

        fetchIntentResult.onSuccess { result ->
            assertCanConfirmIntent(
                intent = result.intent,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = result.clientSecret,
                ),
            )
        }.onFailure { exception ->
            fail(exception.message, exception)
        }
    }

    private fun testWithSetupIntent(
        country: MerchantCountry,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
        shippingDetails: AddressDetails?,
        customerRequestedSave: Boolean,
    ) = runLpmNetworkTest(country = country, allowsManualConfirmation = false) {
        val fetchIntentResult = testClient.createSetupIntent(
            country = country,
            paymentMethodType = paymentMethodType,
        ).mapCatching { clientSecret ->
            FetchIntentResult(
                clientSecret = clientSecret,
                intent = testClient.retrieveSetupIntent(clientSecret).getOrThrow(),
            )
        }

        fetchIntentResult.onSuccess { result ->
            assertCanConfirmIntent(
                initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                    clientSecret = result.clientSecret,
                ),
                intent = result.intent,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingDetails = shippingDetails,
                customerRequestedSave = customerRequestedSave,
            )
        }.onFailure { exception ->
            fail(exception.message, exception)
        }
    }

    private fun testWithDeferredPaymentIntent(
        amount: Int,
        currency: String,
        country: MerchantCountry,
        allowsManualConfirmation: Boolean,
        createWithSetupFutureUsage: Boolean,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
        shippingDetails: AddressDetails?,
        customerRequestedSave: Boolean,
    ) = runLpmNetworkTest(country, allowsManualConfirmation) {
        PaymentElementCallbackReferences.set(
            key = LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
            callbacks = PaymentElementCallbacks(
                createIntentCallback = { paymentMethod, _ ->
                    testClient.createPaymentIntent(
                        country = country,
                        amount = amount,
                        currency = currency,
                        paymentMethodType = paymentMethodType,
                        paymentMethodId = paymentMethod.id,
                        createWithSetupFutureUsage = createWithSetupFutureUsage,
                    ).fold(
                        onSuccess = {
                            CreateIntentResult.Success(it)
                        },
                        onFailure = { exception ->
                            CreateIntentResult.Failure(
                                cause = Exception(exception),
                                displayMessage = exception.message,
                            )
                        }
                    )
                },
                externalPaymentMethodConfirmHandler = null,
            )
        )

        assertCanConfirmIntent(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = amount.toLong(),
                        currency = currency,
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession.takeIf {
                            createWithSetupFutureUsage
                        },
                    )
                )
            ),
            // This intent is never used in the deferred mode so it's safe to make a mocked one here
            intent = PaymentIntentFactory.create(),
            createParams = createParams,
            optionsParams = optionsParams,
            extraParams = extraParams,
            shippingDetails = shippingDetails,
            customerRequestedSave = customerRequestedSave,
        )
    }

    private fun testWithDeferredSetupIntent(
        country: MerchantCountry,
        allowsManualConfirmation: Boolean,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams?,
        extraParams: PaymentMethodExtraParams?,
        shippingDetails: AddressDetails?,
        customerRequestedSave: Boolean,
    ) = runLpmNetworkTest(country, allowsManualConfirmation) {
        PaymentElementCallbackReferences.set(
            key = LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER,
            callbacks = PaymentElementCallbacks(
                createIntentCallback = { paymentMethod, _ ->
                    testClient.createSetupIntent(
                        country = country,
                        paymentMethodType = paymentMethodType,
                        paymentMethodId = paymentMethod.id,
                    ).fold(
                        onSuccess = {
                            CreateIntentResult.Success(it)
                        },
                        onFailure = { exception ->
                            CreateIntentResult.Failure(
                                cause = Exception(exception),
                                displayMessage = exception.message,
                            )
                        }
                    )
                },
                externalPaymentMethodConfirmHandler = null,
            )
        )

        assertCanConfirmIntent(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                        setupFutureUse = PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
                    )
                )
            ),
            // This intent is never used in the deferred mode so it's safe to make a mocked one here
            intent = SetupIntentFactory.create(),
            createParams = createParams,
            optionsParams = optionsParams,
            extraParams = extraParams,
            shippingDetails = shippingDetails,
            customerRequestedSave = customerRequestedSave,
        )
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

    enum class TestType {
        PaymentIntent,
        PaymentIntentWithSetupFutureUsage,
        SetupIntent,
        DeferredPaymentIntent,
        DeferredPaymentIntentWithSetupFutureUsage,
        DeferredSetupIntent,
    }

    private data class FetchIntentResult(
        val clientSecret: String,
        val intent: StripeIntent,
    )

    private companion object {
        const val LPM_NETWORK_PAYMENT_ELEMENT_CALLBACK_TEST_IDENTIFIER = "LpmNetworkTestIdentifier"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
