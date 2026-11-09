package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.LinkSignupDisabledReason
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkSignupModeResult
import com.stripe.android.paymentsheet.state.LinkState
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class LinkAvailabilityLoggerTest {
    @Test
    fun `logs all recorded Link disabled reasons`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(
            linkState = LinkDisabledState(LinkDisabledReason.entries),
        ),
    ) {
        assertThat(logger.messages.awaitItem()).isEqualTo(
            "Payment Element could not offer Link:\n" +
                "  * Link is not supported by this Elements Session.\n" +
                "  * LinkConfiguration.display is set to Never.\n" +
                "  * Card brand filtering requires native Link; the web Link flow does not support it.\n" +
                "  * Collecting additional billing details requires native Link; " +
                "the web Link flow does not support it.\n" +
                "  * Checkout Sessions with automatic tax using the billing address do not support Link.\n" +
                "  * Web Link in passthrough mode requires a customer email for Checkout Sessions. " +
                "Provide a customer email on the Checkout Session or in defaultBillingDetails."
        )
    }

    @Test
    fun `does not invent a reason when Link state is absent`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(linkState = null),
    ) {
        logger.messages.expectNoEvents()
    }

    @Test
    fun `does not log unavailable Link when only inline signup is disabled`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(
            linkState = linkState().copy(
                signupModeResult = LinkSignupModeResult.Disabled(
                    listOf(LinkSignupDisabledReason.DisabledInElementsSession),
                ),
            ),
        ),
    ) {
        logger.messages.expectNoEvents()
    }

    @Test
    fun `explains hidden wallet button without claiming Link is disabled`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(
            linkState = linkState(),
            linkConfiguration = PaymentSheet.LinkConfiguration(
                display = PaymentSheet.LinkConfiguration.Display.WalletButtonHidden,
            ),
        ),
    ) {
        assertThat(logger.messages.awaitItem()).isEqualTo(
            "Payment Element could not show the Link button:\n" +
                "  * LinkConfiguration.display is set to WalletButtonHidden and no existing Link user was found."
        )
    }

    @Test
    fun `does not log a hidden wallet button for an existing Link user`() = runScenario(
        metadata = PaymentMethodMetadataFactory.create(
            linkState = linkState().copy(loginState = LinkState.LoginState.NeedsVerification),
            linkConfiguration = PaymentSheet.LinkConfiguration(
                display = PaymentSheet.LinkConfiguration.Display.WalletButtonHidden,
            ),
        ),
    ) {
        logger.messages.expectNoEvents()
    }

    @Test
    fun `explains Express Checkout Link shipping restriction`() = runScenario(
        metadata = checkoutMetadataRequiringShipping(),
        isExpressCheckoutElement = true,
    ) {
        assertThat(logger.messages.awaitItem()).isEqualTo(
            "Express Checkout Element could not show the Link button:\n" +
                "  * The Link button in Express Checkout Element does not support collecting a shipping address."
        )
    }

    @Test
    fun `does not apply Express Checkout shipping restriction to Payment Element`() = runScenario(
        metadata = checkoutMetadataRequiringShipping(),
    ) {
        logger.messages.expectNoEvents()
    }

    private fun checkoutMetadataRequiringShipping() = PaymentMethodMetadataFactory.create(
        linkState = linkState(),
        integrationMetadata = IntegrationMetadata.CheckoutSession(
            instancesKey = "LinkAvailabilityLoggerTest",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(requiresShippingAddress = true),
            collectedEmail = null,
        ),
    )

    private fun linkState() = LinkState(
        configuration = TestFactory.LINK_CONFIGURATION,
        loginState = LinkState.LoginState.LoggedOut,
        signupModeResult = LinkSignupModeResult.Enabled(LinkSignupMode.InsteadOfSaveForFutureUse, false),
    )

    private fun runScenario(
        metadata: PaymentMethodMetadata,
        isExpressCheckoutElement: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val logger = FakePaymentMethodAvailabilityLogger()
        logUnavailableLink(metadata = metadata, logger = logger, isExpressCheckoutElement = isExpressCheckoutElement)
        Scenario(logger).block()
        logger.messages.ensureAllEventsConsumed()
    }

    private data class Scenario(val logger: FakePaymentMethodAvailabilityLogger)
}
