package com.stripe.android.paymentelement.confirmation.intent

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.data.CustomerSheetDataResult
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetPaymentMethodDataSource
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomerSheetAttachPaymentMethodInterceptorTest {
    @Test
    fun `Rejects new payment method confirmation`() = runTest {
        val interceptor = createInterceptor()

        val result = interceptor.intercept(
            intent = SetupIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.New(
                createParams = com.stripe.android.model.PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                extraParams = null,
                shouldSave = false,
            ),
            shippingValues = null,
        )

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.cause.message)
            .isEqualTo("Cannot use CustomerSheetAttachPaymentMethodInterceptor with new payment methods!")
    }

    @Test
    fun `Rejects non-SetupIntent`() = runTest {
        val interceptor = createInterceptor()

        val result = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(
            ConfirmationHandler.Result.Failed.ErrorType.Internal
        )
        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.cause.message).isEqualTo("Cannot confirm non setup intents with Customer Sheet!")
    }

    @Test
    fun `Successfully attaches payment method`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val setupIntent = SetupIntentFactory.create()

        val interceptor = createInterceptor(
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = { id ->
                    assertThat(id).isEqualTo(paymentMethod.id)
                    CustomerSheetDataResult.success(paymentMethod)
                }
            ),
        )

        val result = interceptor.intercept(
            intent = setupIntent,
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete

        assertThat(completeAction.intent).isEqualTo(setupIntent.copy(paymentMethod = paymentMethod))
        assertThat(completeAction.deferredIntentConfirmationType).isNull()
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `Fails when attach payment method fails`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val errorCause = Exception("Failed to attach payment method")
        val errorMessage = "Unable to attach payment method to customer"

        val interceptor = createInterceptor(
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = { _ ->
                    CustomerSheetDataResult.failure(
                        displayMessage = errorMessage,
                        cause = errorCause,
                    )
                }
            ),
        )

        val result = interceptor.intercept(
            intent = SetupIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail

        assertThat(failAction.message).isEqualTo(errorMessage.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `Logs error when attach payment method fails`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val errorCause = Exception("Failed to attach")

        val logger = FakeLogger()
        val interceptor = createInterceptor(
            paymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
                onAttachPaymentMethod = { _ ->
                    CustomerSheetDataResult.failure(
                        displayMessage = "Error",
                        cause = errorCause,
                    )
                }
            ),
            logger = logger,
        )

        interceptor.intercept(
            intent = SetupIntentFactory.create(),
            confirmationOption = PaymentMethodConfirmationOption.Saved(
                paymentMethod = paymentMethod,
                optionsParams = null,
            ),
            shippingValues = null,
        )

        assertThat(logger.errorLogs).hasSize(1)
        assertThat(logger.errorLogs[0].first).contains("Failed to attach payment method")
        assertThat(logger.errorLogs[0].first).contains(paymentMethod.id)
    }

    private fun createInterceptor(
        paymentMethodDataSource: CustomerSheetPaymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(),
        logger: Logger = FakeLogger(),
    ): CustomerSheetAttachPaymentMethodInterceptor {
        return CustomerSheetAttachPaymentMethodInterceptor(
            paymentMethodDataSourceProvider = { paymentMethodDataSource },
            logger = logger,
        )
    }
}
