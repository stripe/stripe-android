package com.stripe.android.paymentelement.confirmation.lpms

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.utils.IntentConfirmationInterceptorTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.fail

internal open class BaseLpmNetworkTest {
    @get:Rule
    val rules = IntentConfirmationInterceptorTestRule()

    private val component = createComponent()

    fun testWithPaymentIntent(
        amount: Int,
        currency: String,
        paymentMethodTypes: List<String>,
        createWithSetupFutureUsage: Boolean,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ) = runTest {
        val fetchIntentResult = component.client.createPaymentIntent(
            amount = amount,
            currency = currency,
            paymentMethodTypes = paymentMethodTypes,
            createWithSetupFutureUsage = createWithSetupFutureUsage,
        ).mapCatching { clientSecret ->
            FetchIntentResult(
                clientSecret = clientSecret,
                intent = component.client.retrievePaymentIntent(clientSecret).getOrThrow(),
            )
        }

        fetchIntentResult.onSuccess { result ->
            component.assertCanConfirmIntent(
                intent = result.intent,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingValues = shippingValues,
                customerRequestedSave = customerRequestedSave,
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = result.clientSecret,
                ),
            )
        }.onFailure { exception ->
            fail(exception.message, exception)
        }
    }

    fun testWithSetupIntent(
        paymentMethodTypes: List<String>,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ) = runTest {
        val fetchIntentResult = component.client.createSetupIntent(
            paymentMethodTypes = paymentMethodTypes,
        ).mapCatching { clientSecret ->
            FetchIntentResult(
                clientSecret = clientSecret,
                intent = component.client.retrieveSetupIntent(clientSecret).getOrThrow(),
            )
        }

        fetchIntentResult.onSuccess { result ->
            component.assertCanConfirmIntent(
                initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                    clientSecret = result.clientSecret,
                ),
                intent = result.intent,
                createParams = createParams,
                optionsParams = optionsParams,
                extraParams = extraParams,
                shippingValues = shippingValues,
                customerRequestedSave = customerRequestedSave,
            )
        }.onFailure { exception ->
            fail(exception.message, exception)
        }
    }

    fun testWithDeferredPaymentIntent(
        amount: Int,
        currency: String,
        paymentMethodTypes: List<String>,
        createWithSetupFutureUsage: Boolean,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ) = runTest {
        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { paymentMethod, shouldSave ->
            component.client.createPaymentIntent(
                amount = amount,
                currency = currency,
                paymentMethodTypes = paymentMethodTypes,
                paymentMethodId = paymentMethod.id,
                createWithSetupFutureUsage = shouldSave,
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
        }

        component.assertCanConfirmIntent(
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
            shippingValues = shippingValues,
            customerRequestedSave = customerRequestedSave,
        )
    }

    fun testWithDeferredSetupIntent(
        paymentMethodTypes: List<String>,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ) = runTest {
        IntentConfirmationInterceptor.createIntentCallback = CreateIntentCallback { paymentMethod, _ ->
            component.client.createSetupIntent(
                paymentMethodTypes = paymentMethodTypes,
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
        }

        component.assertCanConfirmIntent(
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
            shippingValues = shippingValues,
            customerRequestedSave = customerRequestedSave,
        )
    }

    private fun createComponent(): LpmNetworkTestComponent {
        return DaggerLpmNetworkTestComponent.builder()
            .application(ApplicationProvider.getApplicationContext())
            .publishableKeyProvider {
                "pk_test_ErsyMEOTudSjQR8hh0VrQr5X008sBXGOu6"
            }
            .stripeAccountIdProvider { null }
            .build()
    }

    private suspend fun LpmNetworkTestComponent.assertCanConfirmIntent(
        initializationMode: PaymentElementLoader.InitializationMode,
        intent: StripeIntent,
        createParams: PaymentMethodCreateParams,
        optionsParams: PaymentMethodOptionsParams? = null,
        extraParams: PaymentMethodExtraParams? = null,
        shippingValues: ConfirmPaymentIntentParams.Shipping?,
        customerRequestedSave: Boolean,
    ) {
        val nextStep = interceptor.intercept(
            initializationMode = initializationMode,
            intent = intent,
            paymentMethodCreateParams = createParams,
            paymentMethodOptionsParams = optionsParams,
            paymentMethodExtraParams = extraParams,
            shippingValues = shippingValues,
            customerRequestedSave = customerRequestedSave,
        )

        when (nextStep) {
            is IntentConfirmationInterceptor.NextStep.Confirm -> assertConfirmed(nextStep.confirmParams)
            else -> fail("Should have been a confirm step!")
        }
    }

    private suspend fun LpmNetworkTestComponent.assertConfirmed(
        confirmParams: ConfirmStripeIntentParams
    ) {
        val result = when (confirmParams) {
            is ConfirmSetupIntentParams -> {
                client.confirmSetupIntent(
                    confirmParams = confirmParams,
                )
            }
            is ConfirmPaymentIntentParams -> {
                client.confirmPaymentIntent(
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

    private data class FetchIntentResult(
        val clientSecret: String,
        val intent: StripeIntent,
    )
}
