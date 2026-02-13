package com.stripe.android.paymentelement.confirmation.intent

import androidx.test.core.app.ApplicationProvider
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

    private val clientAttributionMetadata = ClientAttributionMetadata(
        elementsSessionConfigId = "test_session_id",
        paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
        paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
    )

    private val requestOptions = ApiRequest.Options(
        apiKey = "pk_test_123",
    )

    @Test
    fun `intercept with succeeded payment intent returns Complete action`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val succeededPaymentIntent = PaymentIntentFactory.create(
            status = StripeIntent.Status.Succeeded,
        )

        val repository = FakeCheckoutSessionStripeRepository(
            createPaymentMethodResult = Result.success(paymentMethod),
            confirmCheckoutSessionResult = Result.success(
                createCheckoutSessionResponse(succeededPaymentIntent)
            ),
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            checkoutSessionId = "cs_test_123",
            clientAttributionMetadata = clientAttributionMetadata,
            context = ApplicationProvider.getApplicationContext(),
            stripeRepository = repository,
            requestOptions = requestOptions,
        )

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
        assertThat(completeAction.intent).isEqualTo(succeededPaymentIntent)
        assertThat(completeAction.metadata).isEqualTo(
            MutableConfirmationMetadata().apply {
                set(DeferredIntentConfirmationTypeKey, DeferredIntentConfirmationType.Server)
            }
        )
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with requires_action payment intent returns Launch action`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val requiresActionPaymentIntent = PaymentIntentFactory.create(
            status = StripeIntent.Status.RequiresAction,
        )

        val repository = FakeCheckoutSessionStripeRepository(
            createPaymentMethodResult = Result.success(paymentMethod),
            confirmCheckoutSessionResult = Result.success(
                createCheckoutSessionResponse(requiresActionPaymentIntent)
            ),
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            checkoutSessionId = "cs_test_123",
            clientAttributionMetadata = clientAttributionMetadata,
            context = ApplicationProvider.getApplicationContext(),
            stripeRepository = repository,
            requestOptions = requestOptions,
        )

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
    fun `intercept fails when payment method creation fails`() = runTest {
        val error = RuntimeException("Payment method creation failed")
        val repository = FakeCheckoutSessionStripeRepository(
            createPaymentMethodResult = Result.failure(error),
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            checkoutSessionId = "cs_test_123",
            clientAttributionMetadata = clientAttributionMetadata,
            context = ApplicationProvider.getApplicationContext(),
            stripeRepository = repository,
            requestOptions = requestOptions,
        )

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

    @Test
    fun `intercept fails when checkout session confirm fails`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val error = RuntimeException("Checkout session confirmation failed")

        val repository = FakeCheckoutSessionStripeRepository(
            createPaymentMethodResult = Result.success(paymentMethod),
            confirmCheckoutSessionResult = Result.failure(error),
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            checkoutSessionId = "cs_test_123",
            clientAttributionMetadata = clientAttributionMetadata,
            context = ApplicationProvider.getApplicationContext(),
            stripeRepository = repository,
            requestOptions = requestOptions,
        )

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

    @Test
    fun `intercept fails when confirm response has no payment intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val repository = FakeCheckoutSessionStripeRepository(
            createPaymentMethodResult = Result.success(paymentMethod),
            confirmCheckoutSessionResult = Result.success(
                createCheckoutSessionResponse(paymentIntent = null)
            ),
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            checkoutSessionId = "cs_test_123",
            clientAttributionMetadata = clientAttributionMetadata,
            context = ApplicationProvider.getApplicationContext(),
            stripeRepository = repository,
            requestOptions = requestOptions,
        )

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
    fun `intercept with saved payment method returns Fail action`() = runTest {
        val repository = FakeCheckoutSessionStripeRepository()

        val interceptor = CheckoutSessionConfirmationInterceptor(
            checkoutSessionId = "cs_test_123",
            clientAttributionMetadata = clientAttributionMetadata,
            context = ApplicationProvider.getApplicationContext(),
            stripeRepository = repository,
            requestOptions = requestOptions,
        )

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
        assertThat(failAction.cause).isInstanceOf<NotImplementedError>()
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    private fun createCheckoutSessionResponse(paymentIntent: PaymentIntent?): CheckoutSessionResponse {
        return CheckoutSessionResponse(
            id = "cs_test_123",
            amount = 1000L,
            currency = "usd",
            // Confirm responses don't include elements_session
            elementsSession = null,
            paymentIntent = paymentIntent,
        )
    }

    private class FakeCheckoutSessionStripeRepository(
        private val createPaymentMethodResult: Result<PaymentMethod> =
            Result.failure(NotImplementedError()),
        private val confirmCheckoutSessionResult: Result<CheckoutSessionResponse> =
            Result.failure(NotImplementedError()),
    ) : AbsFakeStripeRepository() {

        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): Result<PaymentMethod> {
            return createPaymentMethodResult
        }

        override suspend fun confirmCheckoutSession(
            confirmCheckoutSessionParams: ConfirmCheckoutSessionParams,
            options: ApiRequest.Options,
        ): Result<CheckoutSessionResponse> {
            return confirmCheckoutSessionResult
        }
    }
}
