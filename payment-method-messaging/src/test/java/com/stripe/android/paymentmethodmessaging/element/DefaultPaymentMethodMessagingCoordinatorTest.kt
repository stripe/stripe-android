@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentmethodmessaging.element.analytics.FakeEventReporter
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultPaymentMethodMessagingCoordinatorTest {

    @Test
    fun `configure returns no content if single and multi partner null`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = coordinator.configureCoordinator(ResultType.NO_CONTENT)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.NoContent::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.NoContent::class.java)
        }
    }

    @Test
    fun `configure returns succeeded if single partner is not null`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = coordinator.configureCoordinator(ResultType.SINGLE_PARTNER)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.SinglePartner::class.java)
        }
    }

    @Test
    fun `configure returns succeeded if multi partner is not null`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = coordinator.configureCoordinator(ResultType.MULTI_PARTNER)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)
        }
    }

    @Test
    fun `configure returns failed if call fails`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = coordinator.configureCoordinator(ResultType.FAILURE)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Failed::class.java)
            assertThat((result as? PaymentMethodMessagingElement.ConfigureResult.Failed)?.error?.message).isEqualTo(
                "Price must be non negative"
            )
            expectNoEvents()
        }
    }

    @Test
    fun `sets content to null on failure`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val successfulResult = coordinator.configureCoordinator(ResultType.MULTI_PARTNER)
            assertThat(successfulResult)
                .isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)

            val failedResult = coordinator.configureCoordinator(ResultType.FAILURE)
            assertThat(failedResult).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Failed::class.java)
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `updates messagingContent with new content`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val singlePartnerResult = coordinator.configureCoordinator(ResultType.SINGLE_PARTNER)
            assertThat(singlePartnerResult)
                .isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.SinglePartner::class.java)

            val multiPartnerResult = coordinator.configureCoordinator(ResultType.MULTI_PARTNER)
            assertThat(multiPartnerResult)
                .isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)
        }
    }

    @Test
    fun `sends unexpected error if content type is UnexpectedError`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val errorResult = coordinator.configureCoordinator(ResultType.UNEXPECTED_ERROR)
            assertThat(errorResult)
                .isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.NoContent::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.NoContent::class.java)

            val error = errorReporter.awaitCall()
            assertThat(error).isNotNull()
            assertThat(error.errorEvent).isEqualTo(
                ErrorReporter.UnexpectedErrorEvent.PAYMENT_METHOD_MESSAGING_ELEMENT_UNABLE_TO_PARSE_RESPONSE
            )
            assertThat(error.additionalNonPiiParams["error_message"]).isEqualTo("whoops")
        }
    }

    private class Scenario(
        val coordinator: DefaultPaymentMethodMessagingCoordinator,
        val errorReporter: FakeErrorReporter
    )

    private fun runScenario(
        testBlock: suspend Scenario.() -> Unit
    ) = runTest {
        val repository: StripeRepository = FakeStripeRepository()
        val paymentConfig = { PaymentConfiguration(publishableKey = "key") }
        val errorReporter = FakeErrorReporter()
        val coordinator = DefaultPaymentMethodMessagingCoordinator(
            stripeRepository = repository,
            paymentConfiguration = paymentConfig,
            eventReporter = FakeEventReporter(),
            viewModelScope = CoroutineScope(UnconfinedTestDispatcher()),
            errorReporter = errorReporter
        )

        Scenario(
            coordinator = coordinator,
            errorReporter = errorReporter
        ).testBlock()

        errorReporter.ensureAllEventsConsumed()
    }

    private enum class ResultType {
        MULTI_PARTNER,
        SINGLE_PARTNER,
        NO_CONTENT,
        FAILURE,
        UNEXPECTED_ERROR
    }

    private suspend fun PaymentMethodMessagingCoordinator.configureCoordinator(
        result: ResultType
    ): PaymentMethodMessagingElement.ConfigureResult {
        val config = PaymentMethodMessagingElement.Configuration()
            .currency("usd")
            .countryCode("US")
        when (result) {
            ResultType.MULTI_PARTNER ->
                config.amount(1000L).paymentMethodTypes(listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm))
            ResultType.SINGLE_PARTNER ->
                config.amount(1000L).paymentMethodTypes(listOf(PaymentMethod.Type.Klarna))
            ResultType.NO_CONTENT ->
                config.amount(0L)
            ResultType.FAILURE -> config.amount(-1L)
            ResultType.UNEXPECTED_ERROR -> config.amount(-100L)
        }
        return this.configure(config.build())
    }
}
