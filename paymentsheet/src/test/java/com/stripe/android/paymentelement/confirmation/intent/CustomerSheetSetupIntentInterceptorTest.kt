package com.stripe.android.paymentelement.confirmation.intent

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.data.CustomerSheetDataResult
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetIntentDataSource
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomerSheetSetupIntentInterceptorTest {

    private val clientAttributionMetadata = ClientAttributionMetadata(
        elementsSessionConfigId = "test_session_id",
        paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
        paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
    )

    private val requestOptions = ApiRequest.Options(
        apiKey = "pk_test_123",
    )

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

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail

        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.cause.message)
            .isEqualTo("Cannot use CustomerSheetSetupIntentInterceptor with new payment methods!")
    }

    @Test
    fun `Successfully creates and confirms setup intent`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val setupIntentClientSecret = "seti_1234_secret_5678"
        val setupIntent = SetupIntentFactory.create(clientSecret = setupIntentClientSecret)

        val interceptor = createInterceptor(
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = true,
                onRetrieveSetupIntentClientSecret = {
                    CustomerSheetDataResult.success(setupIntentClientSecret)
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

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<*>>()
    }

    @Test
    fun `Fails when setup intent creation fails`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val errorCause = Exception("Failed to create setup intent")
        val errorMessage = "Unable to create setup intent"

        val interceptor = createInterceptor(
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = true,
                onRetrieveSetupIntentClientSecret = {
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
    fun `Logs error when setup intent creation fails`() = runTest {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val errorCause = Exception("Failed to create setup intent")

        val logger = FakeLogger()

        val interceptor = createInterceptor(
            intentDataSource = FakeCustomerSheetIntentDataSource(
                canCreateSetupIntents = true,
                onRetrieveSetupIntentClientSecret = {
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
        assertThat(logger.errorLogs[0].first).contains("Failed to attach payment method to SetupIntent")
    }

    private fun createInterceptor(
        intentDataSource: CustomerSheetIntentDataSource = FakeCustomerSheetIntentDataSource(),
        logger: Logger = FakeLogger(),
    ): CustomerSheetSetupIntentInterceptor {
        return CustomerSheetSetupIntentInterceptor(
            intentDataSourceProvider = { intentDataSource },
            intentFirstConfirmationInterceptorFactory = FakeIntentFirstConfirmationInterceptorFactory(requestOptions),
            logger = logger,
            clientAttributionMetadata = clientAttributionMetadata,
        )
    }

    private class FakeIntentFirstConfirmationInterceptorFactory(
        private val requestOptions: ApiRequest.Options,
    ) : IntentFirstConfirmationInterceptor.Factory {
        override fun create(
            clientSecret: String,
            clientAttributionMetadata: ClientAttributionMetadata
        ) = IntentFirstConfirmationInterceptor(clientSecret, clientAttributionMetadata, requestOptions)
    }
}
