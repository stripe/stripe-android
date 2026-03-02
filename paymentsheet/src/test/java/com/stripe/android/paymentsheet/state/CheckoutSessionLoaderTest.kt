package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.FakeCheckoutSessionRepository
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class CheckoutSessionLoaderTest {

    @Test
    fun `returns elements session from checkout response`() = runTest {
        val loader = createLoader(
            initResult = Result.success(CHECKOUT_SESSION_RESPONSE),
        )

        val result = loader(INIT_MODE)

        assertThat(result.elementsSession).isEqualTo(CHECKOUT_SESSION_RESPONSE.elementsSession)
    }

    @Test
    fun `returns CheckoutSession customer info when customer present`() = runTest {
        val loader = createLoader(
            initResult = Result.success(CHECKOUT_SESSION_RESPONSE),
        )

        val result = loader(INIT_MODE)

        assertThat(result.customerInfo).isInstanceOf<CustomerInfo.CheckoutSession>()
        val checkoutCustomer = result.customerInfo as CustomerInfo.CheckoutSession
        assertThat(checkoutCustomer.customer.id).isEqualTo("cus_test_123")
    }

    @Test
    fun `returns null customer info when checkout response has no customer`() = runTest {
        val loader = createLoader(
            initResult = Result.success(CHECKOUT_SESSION_RESPONSE.copy(customer = null)),
        )

        val result = loader(INIT_MODE)

        assertThat(result.customerInfo).isNull()
    }

    @Test
    fun `throws when checkout response has no elements session`() = runTest {
        val loader = createLoader(
            initResult = Result.success(CHECKOUT_SESSION_RESPONSE.copy(elementsSession = null)),
        )

        assertFailsWith<IllegalStateException> {
            loader(INIT_MODE)
        }
    }

    @Test
    fun `throws when checkout session repository returns failure`() = runTest {
        val loader = createLoader(
            initResult = Result.failure(RuntimeException("network error")),
        )

        assertFailsWith<RuntimeException> {
            loader(INIT_MODE)
        }
    }

    private fun createLoader(
        initResult: Result<CheckoutSessionResponse>,
    ): CheckoutSessionLoader {
        return CheckoutSessionLoader(
            checkoutSessionRepository = FakeCheckoutSessionRepository(initResult = initResult),
        )
    }

    private companion object {
        private val INIT_MODE = PaymentElementLoader.InitializationMode.CheckoutSession(
            clientSecret = "cs_test_123_secret_abc",
        )

        private val CHECKOUT_SESSION_RESPONSE = CheckoutSessionResponse(
            id = "cs_test_123",
            amount = 5099,
            currency = "usd",
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
