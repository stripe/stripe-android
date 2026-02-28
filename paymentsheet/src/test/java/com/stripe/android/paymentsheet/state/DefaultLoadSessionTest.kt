package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.FakeCheckoutSessionRepository
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultLoadSessionTest {

    @Test
    fun `dispatches to elements session loader for PaymentIntent mode`() = runScenario {
        val result = loadSession(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = null,
        )

        assertThat(result.elementsSession.stripeIntent)
            .isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        assertThat(result.checkoutSession).isNull()
    }

    @Test
    fun `dispatches to checkout session loader for CheckoutSession mode`() = runScenario(
        checkoutSessionRepository = FakeCheckoutSessionRepository(
            initResult = Result.success(CHECKOUT_SESSION_RESPONSE),
        ),
    ) {
        val result = loadSession(
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                clientSecret = "cs_test_123_secret_abc",
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = null,
        )

        assertThat(result.elementsSession).isEqualTo(CHECKOUT_SESSION_RESPONSE.elementsSession)
        assertThat(result.checkoutSession).isEqualTo(CHECKOUT_SESSION_RESPONSE)
    }

    @Test
    fun `passes savedPaymentMethodSelection to elements session repository`() = runScenario {
        val savedSelection = SavedSelection.PaymentMethod(id = "pm_123")

        loadSession(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            configuration = DEFAULT_CONFIG,
            savedPaymentMethodSelection = savedSelection,
        )

        assertThat(elementsSessionRepository.lastParams?.savedPaymentMethodSelectionId)
            .isEqualTo("pm_123")
    }

    private data class Scenario(
        val loadSession: DefaultLoadSession,
        val elementsSessionRepository: FakeElementsSessionRepository,
    )

    private fun runScenario(
        checkoutSessionRepository: FakeCheckoutSessionRepository = FakeCheckoutSessionRepository(),
        sessionsCustomer: ElementsSession.Customer? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            error = null,
            linkSettings = null,
            sessionsCustomer = sessionsCustomer,
        )
        val loader = DefaultLoadSession(
            checkoutSessionLoader = CheckoutSessionLoader(
                checkoutSessionRepository = checkoutSessionRepository,
            ),
            elementsSessionLoader = ElementsSessionLoader(
                elementsSessionRepository = elementsSessionRepository,
            ),
        )
        Scenario(
            loadSession = loader,
            elementsSessionRepository = elementsSessionRepository,
        ).block()
    }

    private companion object {
        private val DEFAULT_CONFIG = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant",
        ).asCommonConfiguration()

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
