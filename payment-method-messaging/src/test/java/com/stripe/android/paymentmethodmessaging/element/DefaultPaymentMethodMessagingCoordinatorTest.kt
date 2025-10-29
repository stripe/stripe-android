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
    private val coordinator = DefaultPaymentMethodMessagingCoordinator(repository, paymentConfig)

    @Test
    fun `configure returns no content if single and multi partner null`() = runTest {
        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(0L)
                .currency("usd")
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.NoContent::class.java)
        coordinator.messagingContent.test {
            val content = awaitItem()
            assertThat(content).isInstanceOf(PaymentMethodMessagingContent.NoContent::class.java)
        }
    }

    @Test
    fun `configure returns succeeded if single partner is not null`() = runTest {
        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(1000L)
                .paymentMethodTypes(listOf(PaymentMethod.Type.Klarna))
                .currency("usd")
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
        coordinator.messagingContent.test {
            val content = awaitItem()
            assertThat(content).isInstanceOf(PaymentMethodMessagingContent.SinglePartner::class.java)
        }
    }

    @Test
    fun `configure returns succeeded if multi partner is not null`() = runTest {
        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(1000L)
                .currency("usd")
                .paymentMethodTypes(listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm))
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
        coordinator.messagingContent.test {
            val content = awaitItem()
            assertThat(content).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)
        }
    }

    @Test
    fun `configure returns failed if call fails`() = runTest {
        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(-1L)
                .currency("usd")
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Failed::class.java)
        assertThat((result as? PaymentMethodMessagingElement.ConfigureResult.Failed)?.error?.message).isEqualTo(
            "Price must be non negative"
        )
        coordinator.messagingContent.test {
            val content = awaitItem()
            assertThat(content).isNull()
        }
    }
}
