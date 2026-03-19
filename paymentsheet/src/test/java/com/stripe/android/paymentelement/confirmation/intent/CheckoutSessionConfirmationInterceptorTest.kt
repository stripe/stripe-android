package com.stripe.android.paymentelement.confirmation.intent

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstancesTestRule
import com.stripe.android.checkout.InternalState
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutSessionConfirmationInterceptorTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    @Test
    fun `intercept with succeeded payment intent returns Complete action`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<PaymentIntent>()
        assertThat((completeAction.intent as PaymentIntent).status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(completeAction.metadata).isEqualTo(
            MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
            }
        )
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with requires_action payment intent returns Launch action`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.testBodyFromFile(
                "checkout-session-confirm.json",
                listOf(REQUIRES_ACTION_REPLACEMENT),
            )
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()

        val launchAction = result as ConfirmationDefinition.Action.Launch
        assertThat(launchAction.launcherArguments).isInstanceOf<IntentConfirmationDefinition.Args.NextAction>()
        assertThat(launchAction.launcherArguments.deferredIntentConfirmationType)
            .isEqualTo(DeferredIntentConfirmationType.Server)
        assertThat(launchAction.receivesResultInProcess).isFalse()
    }

    @Test
    fun `intercept fails when payment method creation fails`() {
        val error = RuntimeException("Payment method creation failed")

        runScenario(
            createPaymentMethodResult = Result.failure(error),
        ) {
            val result = interceptNewPm()

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

            val failAction = result as ConfirmationDefinition.Action.Fail
            assertThat(failAction.cause).isEqualTo(error)
            assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
        }
    }

    @Test
    fun `intercept fails when checkout session confirm fails`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Checkout session confirmation failed"}}""")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `intercept fails when confirm response has no intent`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `intercept with succeeded setup intent returns Complete action`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<SetupIntent>()
        assertThat((completeAction.intent as SetupIntent).status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with both intents prefers paymentIntent`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-both-intents.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<PaymentIntent>()
    }

    @Test
    fun `intercept with requires_action setup intent returns Launch action`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.testBodyFromFile(
                "checkout-session-confirm-setup.json",
                listOf(REQUIRES_ACTION_REPLACEMENT),
            )
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()

        val launchAction = result as ConfirmationDefinition.Action.Launch
        assertThat(launchAction.launcherArguments).isInstanceOf<IntentConfirmationDefinition.Args.NextAction>()
        assertThat(launchAction.launcherArguments.deferredIntentConfirmationType)
            .isEqualTo(DeferredIntentConfirmationType.Server)
        assertThat(launchAction.receivesResultInProcess).isFalse()
    }

    @Test
    fun `intercept with saved payment method and succeeded payment intent returns Complete action`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptSavedPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<PaymentIntent>()
        assertThat((completeAction.intent as PaymentIntent).status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(completeAction.metadata).isEqualTo(
            MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
            }
        )
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with saved payment method and requires_action payment intent returns Launch action`() =
        runScenario {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            ) { response ->
                response.testBodyFromFile(
                    "checkout-session-confirm.json",
                    listOf(REQUIRES_ACTION_REPLACEMENT),
                )
            }

            val result = interceptSavedPm()

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()

            val launchAction = result as ConfirmationDefinition.Action.Launch
            assertThat(launchAction.launcherArguments).isInstanceOf<IntentConfirmationDefinition.Args.NextAction>()
            assertThat(launchAction.launcherArguments.deferredIntentConfirmationType)
                .isEqualTo(DeferredIntentConfirmationType.Server)
            assertThat(launchAction.receivesResultInProcess).isFalse()
        }

    @Test
    fun `intercept with saved payment method fails when checkout session confirm fails`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Checkout session confirmation failed"}}""")
        }

        val result = interceptSavedPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `intercept with new payment method passes shouldSave true when save is enabled and checkbox checked`() =
        runScenario(
            customerMetadata = SAVE_ENABLED_CUSTOMER_METADATA,
        ) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
                bodyPart("save_payment_method", "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptNewPm(shouldSave = true)
        }

    @Test
    fun `intercept with new payment method passes shouldSave false when save is enabled and checkbox unchecked`() =
        runScenario(
            customerMetadata = SAVE_ENABLED_CUSTOMER_METADATA,
        ) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
                bodyPart("save_payment_method", "false"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptNewPm(shouldSave = false)
        }

    @Test
    fun `intercept with new payment method omits savePaymentMethod when save is disabled`() = runScenario(
        customerMetadata = SAVE_DISABLED_CUSTOMER_METADATA,
    ) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            not(hasBodyPart("save_payment_method")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm()
    }

    @Test
    fun `intercept with new payment method omits savePaymentMethod for guest`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            not(hasBodyPart("save_payment_method")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm()
    }

    @Test
    fun `intercept passes expectedAmount from payment intent`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(intent = PaymentIntentFactory.create(amount = 5099L))
    }

    @Test
    fun `intercept omits expectedAmount for setup intent`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            not(hasBodyPart("expected_amount")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        interceptNewPm(intent = SetupIntentFactory.create())
    }

    @Test
    fun `intercept with saved payment method passes null for savePaymentMethod`() = runScenario {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            not(hasBodyPart("save_payment_method")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm()
    }

    @Test
    fun `successful confirm updates multiple Checkout instances`() = runScenario(
        checkoutInstanceCount = 2
    ) {
        val checkout1 = checkoutInstances[0]
        val checkout2 = checkoutInstances[1]
        turbineScope {
            val checkoutSessionTurbine1 = checkout1.checkoutSession.testIn(backgroundScope)
            val checkoutSessionTurbine2 = checkout2.checkoutSession.testIn(backgroundScope)

            assertThat(checkoutSessionTurbine1.awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
            assertThat(checkoutSessionTurbine2.awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptNewPm()

            assertThat(checkoutSessionTurbine1.awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
            assertThat(checkoutSessionTurbine2.awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `successful confirm with new PM updates registered Checkout instances`() = runScenario(
        checkoutInstanceCount = 1
    ) {
        val checkout = checkoutInstances.single()
        checkout.checkoutSession.test {
            assertThat(awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptNewPm()

            assertThat(awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `successful confirm with saved PM updates registered Checkout instances`() = runScenario(
        checkoutInstanceCount = 1
    ) {
        val checkout = checkoutInstances.single()
        checkout.checkoutSession.test {
            assertThat(awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptSavedPm()

            assertThat(awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)
        }
    }

    @Test
    fun `failed confirm does not update registered Checkout instances`() = runScenario(
        checkoutInstanceCount = 1,
    ) {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_pages/$DEFAULT_CHECKOUT_SESSION_ID/confirm"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Checkout session confirmation failed"}}""")
        }

        val checkout = checkoutInstances.single()
        checkout.checkoutSession.test {
            assertThat(awaitItem().id).isEqualTo(DEFAULT_CHECKOUT_SESSION_ID)

            interceptNewPm()

            ensureAllEventsConsumed()
        }
    }

    private fun runScenario(
        createPaymentMethodResult: Result<PaymentMethod> = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        customerMetadata: CustomerMetadata? = null,
        checkoutInstanceCount: Int = 0,
        block: suspend Scenario.() -> Unit,
    ) {
        val stripeRepository = FakeCreatePaymentMethodRepository(
            createPaymentMethodResult = createPaymentMethodResult,
        )

        val checkoutSessionRepository = CheckoutSessionRepository(
            stripeNetworkClient = DefaultStripeNetworkClient(),
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = DEFAULT_CHECKOUT_SESSION_ID,
                instancesKey = INSTANCES_KEY,
            ),
            customerMetadata = customerMetadata,
            clientAttributionMetadata = ClientAttributionMetadata(
                elementsSessionConfigId = "test_session_id",
                paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            ),
            context = applicationContext,
            stripeRepository = stripeRepository,
            checkoutSessionRepository = checkoutSessionRepository,
            requestOptions = ApiRequest.Options(apiKey = "pk_test_123"),
        )

        @Suppress("EmptyRange")
        val checkoutInstances = (0 until checkoutInstanceCount).map {
            Checkout.createWithState(
                context = applicationContext,
                state = Checkout.State(
                    InternalState(
                        key = INSTANCES_KEY,
                        checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
                    ),
                ),
            )
        }

        runTest {
            val scenario = Scenario(
                interceptor = interceptor,
                checkoutInstances = checkoutInstances,
                backgroundScope = backgroundScope,
            )

            scenario.block()
        }
    }

    private data class Scenario(
        val interceptor: CheckoutSessionConfirmationInterceptor,
        val checkoutInstances: List<Checkout>,
        val backgroundScope: CoroutineScope,
    ) {
        suspend fun interceptNewPm(
            shouldSave: Boolean = false,
            intent: StripeIntent = PaymentIntentFactory.create(),
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> = interceptor.intercept(
            intent = intent,
            confirmationOption = NEW_PM_OPTION.copy(shouldSave = shouldSave),
            shippingValues = null,
        )

        suspend fun interceptSavedPm(
            intent: StripeIntent = PaymentIntentFactory.create(),
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> =
            interceptor.intercept(
                intent = intent,
                confirmationOption = SAVED_PM_OPTION,
                shippingValues = null,
            )
    }

    private class FakeCreatePaymentMethodRepository(
        private val createPaymentMethodResult: Result<PaymentMethod> =
            Result.failure(NotImplementedError()),
    ) : AbsFakeStripeRepository() {

        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): Result<PaymentMethod> {
            return createPaymentMethodResult
        }
    }

    private companion object {
        const val INSTANCES_KEY = "CheckoutSessionConfirmationInterceptorTest"

        val REQUIRES_ACTION_REPLACEMENT = ResponseReplacement(
            original = "\"status\": \"succeeded\"",
            new = "\"status\": \"requires_action\"",
        )

        val NEW_PM_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        val SAVED_PM_OPTION = PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
        )

        val SAVE_ENABLED_CUSTOMER_METADATA = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.copy(
            saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
        )

        val SAVE_DISABLED_CUSTOMER_METADATA = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.copy(
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
        )
    }
}
