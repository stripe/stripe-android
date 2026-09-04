package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class DefaultGetGooglePayStateTest {

    @Test
    fun `returns ready and supported when all checks pass`() = runScenario {
        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION,
            initializationMode = INITIALIZATION_MODE,
            isGooglePaySupportedOnDevice = CompletableDeferred(true),
            isGooglePaySupportedByConfiguration = CompletableDeferred(true),
        )

        assertThat(result).isEqualTo(
            GooglePayState(
                isGooglePayReady = true,
                isGooglePaySupported = true,
            )
        )
    }

    @Test
    fun `returns device support independently from readiness`() = runScenario {
        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION,
            initializationMode = INITIALIZATION_MODE,
            isGooglePaySupportedOnDevice = CompletableDeferred(false),
            isGooglePaySupportedByConfiguration = CompletableDeferred(true),
        )

        assertThat(result.isGooglePayReady).isTrue()
        assertThat(result.isGooglePaySupported).isFalse()
    }

    @Test
    fun `returns not ready when Google Pay is disabled for the session`() = runScenario {
        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION.copy(isGooglePayEnabled = false),
            initializationMode = INITIALIZATION_MODE,
            isGooglePaySupportedOnDevice = CompletableDeferred(true),
            isGooglePaySupportedByConfiguration = CompletableDeferred(true),
        )

        assertThat(result.isGooglePayReady).isFalse()
        assertThat(userFacingLogger.getLoggedMessages())
            .contains("Google Pay is not enabled for this session.")
    }

    @Test
    fun `returns not ready when Google Pay is not configured`() = runScenario {
        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION,
            initializationMode = INITIALIZATION_MODE,
            isGooglePaySupportedOnDevice = CompletableDeferred(true),
            isGooglePaySupportedByConfiguration = CompletableDeferred(false),
        )

        assertThat(result.isGooglePayReady).isFalse()
        assertThat(userFacingLogger.getLoggedMessages())
            .contains("GooglePayConfiguration is not set.")
    }

    @Test
    fun `returns not ready when the configured Google Pay check fails`() = runScenario {
        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION,
            initializationMode = INITIALIZATION_MODE,
            isGooglePaySupportedOnDevice = CompletableDeferred(true),
            isGooglePaySupportedByConfiguration = CompletableDeferred(false),
        )

        assertThat(result.isGooglePayReady).isFalse()
        assertThat(
            userFacingLogger.getLoggedMessages().any { it.startsWith("Google Pay API check failed.") }
        ).isTrue()
    }

    @Test
    fun `returns not ready for automatic tax billing without default billing details`() = runScenario {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.newBuilder()
                .defaultBillingDetails(null)
                .build()
                .asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION,
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = "DefaultGetGooglePayStateTest",
                checkoutSessionResponse = checkoutSessionResponse,
            ),
            isGooglePaySupportedOnDevice = CompletableDeferred(true),
            isGooglePaySupportedByConfiguration = CompletableDeferred(true),
        )

        assertThat(result.isGooglePayReady).isFalse()
        assertThat(userFacingLogger.getLoggedMessages()).contains(
            "Google Pay is disabled because automatic tax is configured to use the billing address and no " +
                "default billing address was provided."
        )
    }

    @Test
    fun `keeps Google Pay ready for automatic tax billing with default billing details`() = runScenario {
        val checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.newBuilder()
                .defaultBillingDetails(PaymentSheet.BillingDetails(email = "customer@email.com"))
                .build()
                .asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION,
            initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
                instancesKey = "DefaultGetGooglePayStateTest",
                checkoutSessionResponse = checkoutSessionResponse,
            ),
            isGooglePaySupportedOnDevice = CompletableDeferred(true),
            isGooglePaySupportedByConfiguration = CompletableDeferred(true),
        )

        assertThat(result.isGooglePayReady).isTrue()
    }

    @Test
    fun `reports when device support check has not completed`() = runScenario(expectError = true) {
        val result = getGooglePayState(
            configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.asCommonConfiguration(),
            elementsSession = ELEMENTS_SESSION,
            initializationMode = INITIALIZATION_MODE,
            isGooglePaySupportedOnDevice = CompletableDeferred(),
            isGooglePaySupportedByConfiguration = CompletableDeferred(true),
        )

        assertThat(result.isGooglePaySupported).isFalse()
        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.GOOGLE_PAY_SKIPPED_DURING_LOAD)
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runScenario(
        expectError = false,
        block = block,
    )

    private fun runScenario(
        expectError: Boolean,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val userFacingLogger = FakeUserFacingLogger()
        val errorReporter = FakeErrorReporter()
        val getGooglePayState = DefaultGetGooglePayState(
            userFacingLogger = userFacingLogger,
            errorReporter = errorReporter,
        )

        Scenario(
            getGooglePayState = getGooglePayState,
            userFacingLogger = userFacingLogger,
            errorReporter = errorReporter,
        ).block()

        if (!expectError) {
            assertThat(errorReporter.getLoggedErrors()).isEmpty()
        }
        errorReporter.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val getGooglePayState: DefaultGetGooglePayState,
        val userFacingLogger: FakeUserFacingLogger,
        val errorReporter: FakeErrorReporter,
    )

    private companion object {
        val INITIALIZATION_MODE = PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
        )
        val ELEMENTS_SESSION = ElementsSession(
            linkSettings = null,
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
        )
    }
}
