package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.testing.PaymentMethodFactory
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class CheckoutSessionLoaderTest {

    @Test
    fun `returns elements session from checkout response`() {
        val result = createLoader()(initMode(CHECKOUT_SESSION_RESPONSE))

        assertThat(result).isEqualTo(CHECKOUT_SESSION_RESPONSE.elementsSession)
    }

    @Test
    fun `throws when checkout response has no elements session`() {
        assertFailsWith<IllegalStateException> {
            createLoader()(initMode(CHECKOUT_SESSION_RESPONSE.copy(elementsSession = null)))
        }
    }

    private fun createLoader(): CheckoutSessionLoader {
        return CheckoutSessionLoader()
    }

    private fun initMode(
        response: CheckoutSessionResponse,
    ): PaymentElementLoader.InitializationMode.CheckoutSession {
        return PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = "CheckoutSessionLoaderTest",
            checkoutSessionResponse = response,
        )
    }

    private companion object {
        private val CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponseFactory.create(
            id = DEFAULT_CHECKOUT_SESSION_ID,
            amount = 5099,
            elementsSession = ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                merchantCountry = null,
                isGooglePayEnabled = true,
                sessionsError = null,
                externalPaymentMethodData = null,
                customer = null,
                cardBrandChoice = null,
                customPaymentMethods = emptyList(),
                elementsSessionId = "es_123",
                flags = emptyMap(),
                orderedPaymentMethodTypesAndWallets = listOf("card"),
                experimentsData = null,
                passiveCaptcha = null,
                merchantLogoUrl = null,
                elementsSessionConfigId = "config_123",
                accountId = "acct_123",
                merchantId = "acct_123",
            ),
            customer = CheckoutSessionResponse.Customer(
                id = "cus_test_123",
                paymentMethods = PaymentMethodFactory.cards(2),
                canDetachPaymentMethod = true,
            ),
        )
    }
}
