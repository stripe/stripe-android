package com.stripe.android.paymentelement.confirmation.intent

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetIntentDataSource
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomerSheetConfirmationInterceptorTest {

    private val clientAttributionMetadata = ClientAttributionMetadata(
        elementsSessionConfigId = "test_session_id",
        paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
        paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
    )

    @Test
    fun `Rejects new payment method confirmation`() = test {
        val result = interceptor.intercept(
            intent = SetupIntentFactory.create(),
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

        assertThat(failAction.errorType).isEqualTo(
            ConfirmationHandler.Result.Failed.ErrorType.Internal
        )
        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.cause.message).isEqualTo(
            "Cannot use CustomerSheetConfirmationInterceptor with new payment methods!"
        )
    }

    @Test
    fun `Creates and confirms setup intent when canCreateSetupIntents is true`() = test(
        intentDataSource = FakeCustomerSheetIntentDataSource(
            canCreateSetupIntents = true,
        ),
        setupInterceptAction = ConfirmationDefinition.Action.Complete(
            intent = SetupIntentFactory.create().copy(paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            deferredIntentConfirmationType = null,
            completedFullPaymentFlow = true,
        ),
    ) {
        val setupIntent = SetupIntentFactory.create()
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val result = interceptor.intercept(
            intent = setupIntent,
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val factoryCall = createSetupIntentInterceptorFactoryCalls.awaitItem()
        assertThat(factoryCall.clientAttributionMetadata).isEqualTo(clientAttributionMetadata)

        val interceptCall = createSetupIntentInterceptCalls.awaitItem()
        assertThat(interceptCall.intent).isEqualTo(setupIntent)
        assertThat(interceptCall.confirmationOption.paymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `Attaches payment method when canCreateSetupIntents is false`() = test(
        intentDataSource = FakeCustomerSheetIntentDataSource(
            canCreateSetupIntents = false,
        ),
        attachInterceptAction = ConfirmationDefinition.Action.Complete(
            intent = SetupIntentFactory.create().copy(paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            deferredIntentConfirmationType = null,
            completedFullPaymentFlow = true,
        ),
    ) {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val setupIntent = SetupIntentFactory.create()

        val result = interceptor.intercept(
            intent = setupIntent,
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete

        assertThat(completeAction.intent).isEqualTo(setupIntent.copy(paymentMethod = paymentMethod))
        assertThat(completeAction.deferredIntentConfirmationType).isNull()
        assertThat(completeAction.completedFullPaymentFlow).isTrue()

        createAttachPaymentMethodInterceptorFactoryCalls.awaitItem()

        val interceptCall = createAttachPaymentMethodInterceptCalls.awaitItem()
        assertThat(interceptCall.intent).isEqualTo(setupIntent)
        assertThat(interceptCall.confirmationOption.paymentMethod).isEqualTo(paymentMethod)
    }

    @Test
    fun `Fails when setup intent creation fails`() = test(
        intentDataSource = FakeCustomerSheetIntentDataSource(
            canCreateSetupIntents = true,
        ),
        setupInterceptAction = ConfirmationDefinition.Action.Fail(
            cause = Exception("Failed to create setup intent"),
            message = "Unable to create setup intent".resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
        ),
    ) {
        val result = interceptor.intercept(
            intent = SetupIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.message).isEqualTo("Unable to create setup intent".resolvableString)
        assertThat(failAction.errorType).isEqualTo(
            ConfirmationHandler.Result.Failed.ErrorType.Payment
        )

        assertThat(createSetupIntentInterceptorFactoryCalls.awaitItem()).isNotNull()
        assertThat(createSetupIntentInterceptCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `Fails when attach payment method fails`() = test(
        intentDataSource = FakeCustomerSheetIntentDataSource(
            canCreateSetupIntents = false,
        ),
        attachInterceptAction = ConfirmationDefinition.Action.Fail(
            cause = Exception("Failed to attach payment method"),
            message = "Unable to attach payment method to customer".resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Payment,
        ),
    ) {
        val result = interceptor.intercept(
            intent = SetupIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.message).isEqualTo("Unable to attach payment method to customer".resolvableString)
        assertThat(failAction.errorType).isEqualTo(
            ConfirmationHandler.Result.Failed.ErrorType.Payment
        )

        assertThat(createAttachPaymentMethodInterceptorFactoryCalls.awaitItem()).isNotNull()
        assertThat(createAttachPaymentMethodInterceptCalls.awaitItem()).isNotNull()
    }

    private fun test(
        intentDataSource: CustomerSheetIntentDataSource = FakeCustomerSheetIntentDataSource(),
        setupInterceptAction: ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> =
            ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException("No action!"),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                message = "No action!".resolvableString
            ),
        attachInterceptAction: ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> =
            ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException("No action!"),
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
                message = "No action!".resolvableString
            ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val setupIntentInterceptor = FakeIntentConfirmationInterceptor(setupInterceptAction)
        val attachPaymentMethodInterceptor = FakeIntentConfirmationInterceptor(attachInterceptAction)
        val setupIntentInterceptorFactory = FakeSetupIntentInterceptorFactory(setupIntentInterceptor)
        val attachPaymentMethodInterceptorFactory =
            FakeAttachPaymentMethodInterceptorFactory(attachPaymentMethodInterceptor)

        val interceptor = CustomerSheetConfirmationInterceptor(
            clientAttributionMetadata = clientAttributionMetadata,
            intentDataSourceProvider = { intentDataSource },
            setupIntentInterceptorFactory = setupIntentInterceptorFactory,
            attachPaymentMethodInterceptorFactory = attachPaymentMethodInterceptorFactory,
        )

        val scenario = Scenario(
            interceptor = interceptor,
            createSetupIntentInterceptorFactoryCalls = setupIntentInterceptorFactory.createCalls,
            createAttachPaymentMethodInterceptorFactoryCalls = attachPaymentMethodInterceptorFactory.createCalls,
            createSetupIntentInterceptCalls = setupIntentInterceptor.interceptCalls,
            createAttachPaymentMethodInterceptCalls = attachPaymentMethodInterceptor.interceptCalls,
        )

        block(scenario)

        scenario.createSetupIntentInterceptorFactoryCalls.ensureAllEventsConsumed()
        scenario.createAttachPaymentMethodInterceptorFactoryCalls.ensureAllEventsConsumed()
        scenario.createSetupIntentInterceptCalls.ensureAllEventsConsumed()
        scenario.createAttachPaymentMethodInterceptCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val interceptor: CustomerSheetConfirmationInterceptor,
        val createSetupIntentInterceptorFactoryCalls: Turbine<FakeSetupIntentInterceptorFactory.CreateCall>,
        val createAttachPaymentMethodInterceptorFactoryCalls: Turbine<Unit>,
        val createSetupIntentInterceptCalls: Turbine<FakeIntentConfirmationInterceptor.InterceptCall>,
        val createAttachPaymentMethodInterceptCalls: Turbine<FakeIntentConfirmationInterceptor.InterceptCall>,
    )

    private class FakeIntentConfirmationInterceptor(
        private val interceptAction: ConfirmationDefinition.Action<IntentConfirmationDefinition.Args>,
    ) : IntentConfirmationInterceptor {
        val interceptCalls = Turbine<InterceptCall>()

        override suspend fun intercept(
            intent: StripeIntent,
            confirmationOption: PaymentMethodConfirmationOption.New,
            shippingValues: ConfirmPaymentIntentParams.Shipping?
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
            throw IllegalStateException("Should call new payment method path in interceptor tests")
        }

        override suspend fun intercept(
            intent: StripeIntent,
            confirmationOption: PaymentMethodConfirmationOption.Saved,
            shippingValues: ConfirmPaymentIntentParams.Shipping?
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> {
            interceptCalls.add(
                InterceptCall(
                    intent = intent,
                    confirmationOption = confirmationOption,
                    shippingValues = shippingValues,
                )
            )

            return interceptAction
        }

        class InterceptCall(
            val intent: StripeIntent,
            val confirmationOption: PaymentMethodConfirmationOption.Saved,
            val shippingValues: ConfirmPaymentIntentParams.Shipping?
        )
    }

    private class FakeSetupIntentInterceptorFactory(
        private val interceptor: IntentConfirmationInterceptor
    ) : CustomerSheetSetupIntentInterceptor.Factory {
        val createCalls = Turbine<CreateCall>()

        override fun create(
            clientAttributionMetadata: ClientAttributionMetadata
        ): IntentConfirmationInterceptor {
            createCalls.add(CreateCall(clientAttributionMetadata))

            return interceptor
        }

        class CreateCall(
            val clientAttributionMetadata: ClientAttributionMetadata
        )
    }

    private class FakeAttachPaymentMethodInterceptorFactory(
        private val interceptor: IntentConfirmationInterceptor
    ) : CustomerSheetAttachPaymentMethodInterceptor.Factory {
        val createCalls = Turbine<Unit>()

        override fun create(): IntentConfirmationInterceptor {
            createCalls.add(Unit)

            return interceptor
        }
    }
}
