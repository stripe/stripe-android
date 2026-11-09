package com.stripe.android.lpmfoundations.paymentmethod

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class PaymentMethodAvailabilityLoggingTest {
    @Test
    fun `logs all unmet requirements for a payment method`() = runScenario(
        PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "us_bank_account"),
                paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"microdeposits"}}""",
            ),
            allowsDelayedPaymentMethods = false,
            financialConnectionsAvailability = null,
        ),
    ) {
        assertThat(logger.messages.awaitItem()).isEqualTo(
            "Payment Element could not offer us_bank_account:\n" +
                "  * The Financial Connections SDK must be included. " +
                "See https://docs.stripe.com/payments/accept-a-payment?" +
                "platform=android&payment-ui=mobile#android-ach\n" +
                "  * US bank accounts require an automatic, instant, or instant_or_skip verification method.\n" +
                "  * Configuration.allowsDelayedPaymentMethods must be set to true."
        )
        assertThat(metadata.supportedPaymentMethodTypes()).containsExactly("card")
    }

    @Test
    fun `logs unmet requirements for unactivated methods`() = runScenario(
        PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "sepa_debit"),
                unactivatedPaymentMethods = listOf("sepa_debit"),
                isLiveMode = true,
            ),
            allowsDelayedPaymentMethods = false,
        ),
    ) {
        assertThat(logger.messages.awaitItem()).isEqualTo(
            "Payment Element could not offer sepa_debit:\n" +
                "  * Configuration.allowsDelayedPaymentMethods must be set to true."
        )
    }

    @Test
    fun `logs setup restrictions`() = runScenario(
        PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "affirm"),
            ),
        ),
    ) {
        assertThat(logger.messages.awaitItem()).isEqualTo(
            "Payment Element could not offer affirm:\n" +
                "  * This payment method is not supported with a SetupIntent or setup_future_usage."
        )
    }

    @Test
    fun `does not duplicate activation warnings for otherwise supported methods`() = runScenario(
        PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
                unactivatedPaymentMethods = listOf("klarna"),
                isLiveMode = true,
            ),
        ),
    ) {
        assertThat(metadata.supportedPaymentMethodTypes()).doesNotContain("klarna")
        logger.messages.expectNoEvents()
    }

    @Test
    fun `does not log unactivated methods in test mode`() = runScenario(
        PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
                unactivatedPaymentMethods = listOf("klarna"),
                isLiveMode = false,
            ),
        ),
    ) {
        assertThat(metadata.supportedPaymentMethodTypes()).containsExactly("card", "klarna")
        logger.messages.expectNoEvents()
    }

    @Test
    fun `logs methods without a supported definition`() = runScenario(
        PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "unknown_method"),
            ),
        ),
    ) {
        assertThat(logger.messages.awaitItem()).isEqualTo(
            "Payment Element could not offer unknown_method:\n" +
                "  * This payment method is not currently supported by the Payment Element."
        )
    }

    @Test
    fun `does not log supported methods or Link handled separately`() = runScenario(
        PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "link"),
            ),
        ),
    ) {
        logger.messages.expectNoEvents()
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val logger = FakePaymentMethodAvailabilityLogger()
        logUnavailablePaymentMethods(
            metadata = metadata,
            logger = logger,
            paymentMethodTypes = metadata.stripeIntent.paymentMethodTypes,
        )
        Scenario(metadata, logger).block()
        logger.messages.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val metadata: PaymentMethodMetadata,
        val logger: FakePaymentMethodAvailabilityLogger,
    )
}

internal class FakePaymentMethodAvailabilityLogger : UserFacingLogger {
    val messages = Turbine<String>()

    override fun logWarningWithoutPii(message: String) {
        messages.add(message)
    }
}
