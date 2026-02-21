package com.stripe.android.paymentelement.confirmation.intent

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.isInstanceOf
import com.stripe.android.model.CheckoutSessionResponse
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmCheckoutSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.MutableConfirmationMetadata
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutSessionConfirmationInterceptorTest {

    @Test
    fun `intercept with succeeded payment intent returns Complete action`() = runScenario(
        createPaymentMethodResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        confirmCheckoutSessionResult = Result.success(
            createCheckoutSessionResponse(
                PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
            )
        ),
    ) {
        val result = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = false,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isEqualTo(
            PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
        )
        assertThat(completeAction.metadata).isEqualTo(
            MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
            }
        )
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with requires_action payment intent returns Launch action`() = runScenario(
        createPaymentMethodResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        confirmCheckoutSessionResult = Result.success(
            createCheckoutSessionResponse(
                PaymentIntentFactory.create(status = StripeIntent.Status.RequiresAction)
            )
        ),
    ) {
        val result = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = false,
            ),
            shippingValues = null,
        )

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
            val result = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    optionsParams = null,
                    extraParams = null,
                    shouldSave = false,
                ),
                shippingValues = null,
            )

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

            val failAction = result as ConfirmationDefinition.Action.Fail
            assertThat(failAction.cause).isEqualTo(error)
            assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
        }
    }

    @Test
    fun `intercept fails when checkout session confirm fails`() {
        val error = RuntimeException("Checkout session confirmation failed")

        runScenario(
            createPaymentMethodResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            confirmCheckoutSessionResult = Result.failure(error),
        ) {
            val result = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.New(
                    createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    optionsParams = null,
                    extraParams = null,
                    shouldSave = false,
                ),
                shippingValues = null,
            )

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

            val failAction = result as ConfirmationDefinition.Action.Fail
            assertThat(failAction.cause).isEqualTo(error)
            assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
        }
    }

    @Test
    fun `intercept fails when confirm response has no payment intent`() = runScenario(
        createPaymentMethodResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        confirmCheckoutSessionResult = Result.success(
            createCheckoutSessionResponse(paymentIntent = null)
        ),
    ) {
        val result = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = false,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `intercept with saved payment method and succeeded payment intent returns Complete action`() = runScenario(
        confirmCheckoutSessionResult = Result.success(
            createCheckoutSessionResponse(
                PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
            )
        ),
    ) {
        val result = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isEqualTo(
            PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
        )
        assertThat(completeAction.metadata).isEqualTo(
            MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
            }
        )
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with saved payment method and requires_action payment intent returns Launch action`() =
        runScenario(
            confirmCheckoutSessionResult = Result.success(
                createCheckoutSessionResponse(
                    PaymentIntentFactory.create(status = StripeIntent.Status.RequiresAction)
                )
            ),
        ) {
            val result = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    optionsParams = null,
                ),
                shippingValues = null,
            )

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()

            val launchAction = result as ConfirmationDefinition.Action.Launch
            assertThat(launchAction.launcherArguments).isInstanceOf<IntentConfirmationDefinition.Args.NextAction>()
            assertThat(launchAction.launcherArguments.deferredIntentConfirmationType)
                .isEqualTo(DeferredIntentConfirmationType.Server)
            assertThat(launchAction.receivesResultInProcess).isFalse()
        }

    @Test
    fun `intercept with saved payment method fails when checkout session confirm fails`() {
        val error = RuntimeException("Checkout session confirmation failed")

        runScenario(
            confirmCheckoutSessionResult = Result.failure(error),
        ) {
            val result = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    optionsParams = null,
                ),
                shippingValues = null,
            )

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

            val failAction = result as ConfirmationDefinition.Action.Fail
            assertThat(failAction.cause).isEqualTo(error)
            assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
        }
    }

    @Test
    fun `intercept with new payment method passes shouldSave true when save checkbox checked`() = runScenario(
        createPaymentMethodResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        confirmCheckoutSessionResult = Result.success(
            createCheckoutSessionResponse(
                PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
            )
        ),
    ) {
        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = true,
            ),
            shippingValues = null,
        )

        val params = confirmCheckoutSessionCalls.awaitItem().toParamMap()
        assertThat(params["save_payment_method"]).isEqualTo(true)
    }

    @Test
    fun `intercept with new payment method passes shouldSave false when save checkbox unchecked`() = runScenario(
        createPaymentMethodResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        confirmCheckoutSessionResult = Result.success(
            createCheckoutSessionResponse(
                PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
            )
        ),
    ) {
        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = false,
            ),
            shippingValues = null,
        )

        val params = confirmCheckoutSessionCalls.awaitItem().toParamMap()
        assertThat(params["save_payment_method"]).isEqualTo(false)
    }

    @Test
    fun `intercept with saved payment method passes null for savePaymentMethod`() = runScenario(
        confirmCheckoutSessionResult = Result.success(
            createCheckoutSessionResponse(
                PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
            )
        ),
    ) {
        interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        val params = confirmCheckoutSessionCalls.awaitItem().toParamMap()
        assertThat(params).doesNotContainKey("save_payment_method")
    }

    private fun runScenario(
        createPaymentMethodResult: Result<PaymentMethod> = Result.failure(NotImplementedError()),
        confirmCheckoutSessionResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
        block: suspend Scenario.() -> Unit,
    ) {
        val confirmCheckoutSessionCalls = Turbine<ConfirmCheckoutSessionParams>()

        val repository = FakeCheckoutSessionStripeRepository(
            createPaymentMethodResult = createPaymentMethodResult,
            confirmCheckoutSessionResult = confirmCheckoutSessionResult,
            confirmCheckoutSessionCalls = confirmCheckoutSessionCalls,
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            checkoutSessionId = "cs_test_123",
            clientAttributionMetadata = ClientAttributionMetadata(
                elementsSessionConfigId = "test_session_id",
                paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
            ),
            context = ApplicationProvider.getApplicationContext(),
            stripeRepository = repository,
            requestOptions = ApiRequest.Options(apiKey = "pk_test_123"),
        )

        val scenario = Scenario(
            interceptor = interceptor,
            confirmCheckoutSessionCalls = confirmCheckoutSessionCalls,
        )

        runTest {
            scenario.block()
        }
    }

    private data class Scenario(
        val interceptor: CheckoutSessionConfirmationInterceptor,
        val confirmCheckoutSessionCalls: ReceiveTurbine<ConfirmCheckoutSessionParams>,
    )

    private fun createCheckoutSessionResponse(paymentIntent: PaymentIntent?): CheckoutSessionResponse {
        return CheckoutSessionResponse(
            id = "cs_test_123",
            amount = 1000L,
            currency = "usd",
            elementsSession = null,
            paymentIntent = paymentIntent,
        )
    }

    private class FakeCheckoutSessionStripeRepository(
        private val createPaymentMethodResult: Result<PaymentMethod> =
            Result.failure(NotImplementedError()),
        private val confirmCheckoutSessionResult: Result<CheckoutSessionResponse> =
            Result.failure(NotImplementedError()),
        private val confirmCheckoutSessionCalls: Turbine<ConfirmCheckoutSessionParams> = Turbine(),
    ) : AbsFakeStripeRepository() {

        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): Result<PaymentMethod> {
            return createPaymentMethodResult
        }

        override suspend fun confirmCheckoutSession(
            checkoutSessionId: String,
            confirmCheckoutSessionParams: ConfirmCheckoutSessionParams,
            options: ApiRequest.Options,
        ): Result<CheckoutSessionResponse> {
            confirmCheckoutSessionCalls.add(confirmCheckoutSessionParams)
            return confirmCheckoutSessionResult
        }
    }
}
