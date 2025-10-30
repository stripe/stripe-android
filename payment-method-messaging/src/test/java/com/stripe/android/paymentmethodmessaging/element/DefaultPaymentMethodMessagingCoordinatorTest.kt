@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultPaymentMethodMessagingCoordinatorTest {

    private val repository: StripeRepository = FakeStripeRepository()
    private val paymentConfig = PaymentConfiguration(publishableKey = "key")
    private val launcher = DefaultLearnMoreActivityLauncher()
    private val coordinator = DefaultPaymentMethodMessagingCoordinator(repository, paymentConfig, launcher)

    @Test
    fun `configure returns no content if single and multi partner null`() = runTest {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = configureCoordinator(ResultType.NO_CONTENT)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.NoContent::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.NoContent::class.java)
        }
    }

    @Test
    fun `configure returns succeeded if single partner is not null`() = runTest {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = configureCoordinator(ResultType.SINGLE_PARTNER)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.SinglePartner::class.java)
        }
    }

    @Test
    fun `configure returns succeeded if multi partner is not null`() = runTest {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = configureCoordinator(ResultType.MULTI_PARTNER)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)
        }
    }

    @Test
    fun `configure returns failed if call fails`() = runTest {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val result = configureCoordinator(ResultType.FAILURE)
            assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Failed::class.java)
            assertThat((result as? PaymentMethodMessagingElement.ConfigureResult.Failed)?.error?.message).isEqualTo(
                "Price must be non negative"
            )
            expectNoEvents()
        }
    }

    @Test
    fun `sets content to null on failure`() = runTest {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val successfulResult = configureCoordinator(ResultType.MULTI_PARTNER)
            assertThat(successfulResult)
                .isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)

            val failedResult = configureCoordinator(ResultType.FAILURE)
            assertThat(failedResult).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Failed::class.java)
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `updates messagingContent with new content`() = runTest {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()

            val singlePartnerResult = configureCoordinator(ResultType.SINGLE_PARTNER)
            assertThat(singlePartnerResult)
                .isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.SinglePartner::class.java)

            val multiPartnerResult = configureCoordinator(ResultType.MULTI_PARTNER)
            assertThat(multiPartnerResult)
                .isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
            assertThat(awaitItem()).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)
        }
    }

    private enum class ResultType {
        MULTI_PARTNER,
        SINGLE_PARTNER,
        NO_CONTENT,
        FAILURE
    }

    private suspend fun configureCoordinator(result: ResultType): PaymentMethodMessagingElement.ConfigureResult {
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
        }
        return coordinator.configure(config.build())
    }
}
