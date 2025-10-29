@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageImage
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessageMultiPartner
import com.stripe.android.model.PaymentMethodMessageSinglePartner
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class DefaultPaymentMethodMessagingCoordinatorTest {

    private val repository: StripeRepository = mock()
    private val paymentConfig = PaymentConfiguration(publishableKey = "key")
    private val coordinator = DefaultPaymentMethodMessagingCoordinator(repository, paymentConfig)

    @Test
    fun `configure returns no content if single and multi partner null`() = runTest {
        whenever(
            repository.retrievePaymentMethodMessage(
                paymentMethods = any(),
                amount = any(),
                currency = any(),
                country = any(),
                locale = any(),
                requestOptions = any()
            )
        ).thenReturn(
            Result.success(
                PaymentMethodMessage(
                    paymentMethods = listOf(),
                    singlePartner = null,
                    multiPartner = null
                )
            )
        )

        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(1000L)
                .currency("usd")
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.NoContent::class.java)
    }

    @Test
    fun `configure returns succeeded if single partner is not null`() = runTest {
        whenever(
            repository.retrievePaymentMethodMessage(
                paymentMethods = any(),
                amount = any(),
                currency = any(),
                country = any(),
                locale = any(),
                requestOptions = any()
            )
        ).thenReturn(
            Result.success(
                PaymentMethodMessage(
                    paymentMethods = listOf(),
                    singlePartner = singlePartner,
                    multiPartner = null
                )
            )
        )

        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(1000L)
                .currency("usd")
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
    }

    @Test
    fun `configure returns succeeded if multi partner is not null`() = runTest {
        whenever(
            repository.retrievePaymentMethodMessage(
                paymentMethods = any(),
                amount = any(),
                currency = any(),
                country = any(),
                locale = any(),
                requestOptions = any()
            )
        ).thenReturn(
            Result.success(
                PaymentMethodMessage(
                    paymentMethods = listOf(),
                    singlePartner = null,
                    multiPartner = multiPartner
                )
            )
        )

        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(1000L)
                .currency("usd")
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Succeeded::class.java)
    }

    @Test
    fun `configure returns failed if call fails`() = runTest {
        whenever(
            repository.retrievePaymentMethodMessage(
                any(), any(), any(), any(), any(), any()
            )
        ).thenReturn(
            Result.failure(
                Exception("Something went wrong")
            )
        )

        val result = coordinator.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .amount(1000L)
                .currency("usd")
                .countryCode("US")
                .build()
        )

        assertThat(result).isInstanceOf(PaymentMethodMessagingElement.ConfigureResult.Failed::class.java)
        assertThat((result as? PaymentMethodMessagingElement.ConfigureResult.Failed)?.error?.message).isEqualTo(
            "Something went wrong"
        )
    }

    private companion object {
        val image = PaymentMethodMessageImage(
            role = "logo",
            url = "www.test.com",
            paymentMethodType = "klarna",
            text = "howdy"
        )
        val learnMore = PaymentMethodMessageLearnMore(
            message = "learn more",
            url = "www.test.com"
        )
        val singlePartner = PaymentMethodMessageSinglePartner(
            inlinePartnerPromotion = "buy stuff",
            lightImage = image,
            darkImage = image,
            flatImage = image,
            learnMore = learnMore
        )
        val multiPartner = PaymentMethodMessageMultiPartner(
            promotion = "buy stuff",
            lightImages = listOf(image),
            darkImages = listOf(image),
            flatImages = listOf(image),
            learnMore = learnMore
        )
    }
}
