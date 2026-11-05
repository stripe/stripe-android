package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultPreselectLinkExperimentTest {
    @Test
    fun `treatment selects Link with its brand and reports exposure data`() = runScenario(
        metadata = metadata().copy(
            linkStateResult = linkState(configurationBrand = LinkBrand.Onelink),
        ),
    ) {
        assertThat(getSelection()).isEqualTo(PaymentSelection.Link(brand = LinkBrand.Onelink))

        val call = eventReporter.experimentExposureCalls.awaitItem()
        assertThat(call.paymentMethodMetadata).isEqualTo(metadata)
        assertThat(call.experiment.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK)
        assertThat(call.experiment.arbId).isEqualTo("arb_123")
        assertThat(call.experiment.group).isEqualTo("treatment")
        assertThat(call.experiment.dimensions).containsExactly(
            "displayed_payment_method_types", "card",
            "displayed_payment_method_types_including_wallets", "card,link",
            "in_app_elements_integration_type", "flowcontroller",
            "link_enabled", "true",
            "link_login_state", "logged_out",
            "google_pay_enabled", "false",
            "has_saved_payment_method", "false",
        )
    }

    @Test
    fun `control does not select Link but reports exposure`() = testControlVariant("control")

    @Test
    fun `control_test does not select Link but reports exposure`() = testControlVariant("control_test")

    @Test
    fun `unsupported variant does not select Link or report exposure`() = runScenario(
        metadata = metadata(variant = "unknown"),
    ) {
        assertNoSelectionOrExposure()
    }

    @Test
    fun `missing experiments data does not select Link or report exposure`() = runScenario(
        metadata = metadata().copy(experimentsData = null),
    ) {
        assertNoSelectionOrExposure()
    }

    @Test
    fun `missing assignment does not select Link or report exposure`() = runScenario(
        metadata = metadata().copy(experimentsData = ElementsSession.ExperimentsData("arb_123", emptyMap())),
    ) {
        assertNoSelectionOrExposure()
    }

    @Test
    fun `PaymentSheet does not select Link or report exposure`() = runScenario(mode = EventReporter.Mode.Complete) {
        assertNoSelectionOrExposure()
    }

    @Test
    fun `Embedded can select Link`() = runScenario(
        mode = EventReporter.Mode.Embedded,
        integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
            isRowSelectionImmediateAction = false,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant").build(),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        assertThat(getSelection()).isEqualTo(PaymentSelection.Link(brand = LinkBrand.Link))
        assertThat(eventReporter.experimentExposureCalls.awaitItem().experiment.dimensions)
            .containsEntry("in_app_elements_integration_type", "embedded")
    }

    @Test
    fun `unavailable Link does not select Link or report exposure`() = runScenario(
        metadata = metadata().copy(linkStateResult = null),
    ) {
        assertNoSelectionOrExposure()
    }

    @Test
    fun `WalletButtonHidden does not select Link or report exposure`() = testHiddenLink(
        PaymentSheet.LinkConfiguration.Display.WalletButtonHidden,
    )

    @Test
    fun `Never display does not select Link or report exposure`() = testHiddenLink(
        PaymentSheet.LinkConfiguration.Display.Never,
    )

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `external wallet buttons do not select Link or report exposure`() = runScenario(
        integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(
            PaymentSheet.Configuration.Builder("Merchant")
                .walletButtons(PaymentSheet.WalletButtonsConfiguration(willDisplayExternally = true))
                .build(),
        ),
    ) {
        assertNoSelectionOrExposure()
    }

    @Test
    fun `ExpressCheckoutElement does not select Link or report exposure`() = runScenario(
        integrationConfiguration = PaymentElementLoader.Configuration.ExpressCheckoutElement(
            PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
        ),
    ) {
        assertNoSelectionOrExposure()
    }

    @Test
    fun `exposure reports logged in Link state`() = testLoginState(LinkState.LoginState.LoggedIn, "logged_in")

    @Test
    fun `exposure reports Link state needing verification`() = testLoginState(
        LinkState.LoginState.NeedsVerification,
        "needs_verification",
    )

    @Test
    fun `exposure reports Link state needing web verification`() = testLoginState(
        LinkState.LoginState.NeedsWebVerification,
        "needs_web_verification",
    )

    @Test
    fun `exposure reports saved payment method availability`() = runScenario {
        val customer = CustomerState(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            defaultPaymentMethodId = null,
        )
        assertThat(getSelection(customer)).isNotNull()
        assertThat(eventReporter.experimentExposureCalls.awaitItem().experiment.dimensions)
            .containsEntry("has_saved_payment_method", "true")
    }

    @Test
    fun `exposure reports Google Pay availability`() = runScenario(
        metadata = metadata().copy(isGooglePayReady = true),
    ) {
        assertThat(getSelection()).isNotNull()
        assertThat(eventReporter.experimentExposureCalls.awaitItem().experiment.dimensions)
            .containsEntry("google_pay_enabled", "true")
    }

    private fun testControlVariant(variant: String) = runScenario(metadata = metadata(variant)) {
        assertThat(getSelection()).isNull()
        assertThat(eventReporter.experimentExposureCalls.awaitItem().experiment.group).isEqualTo(variant)
    }

    private fun testHiddenLink(display: PaymentSheet.LinkConfiguration.Display) = runScenario(
        integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(
            PaymentSheet.Configuration.Builder("Merchant")
                .link(PaymentSheet.LinkConfiguration(display = display))
                .build(),
        ),
    ) {
        assertNoSelectionOrExposure()
    }

    private fun testLoginState(loginState: LinkState.LoginState, expectedDimension: String) = runScenario(
        metadata = metadata().copy(linkStateResult = linkState(loginState = loginState)),
    ) {
        assertThat(getSelection()).isNotNull()
        assertThat(eventReporter.experimentExposureCalls.awaitItem().experiment.dimensions)
            .containsEntry("link_login_state", expectedDimension)
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = metadata(),
        mode: EventReporter.Mode = EventReporter.Mode.Custom,
        integrationConfiguration: PaymentElementLoader.Configuration = PaymentElementLoader.Configuration.PaymentSheet(
            PaymentSheetFixtures.CONFIG_MINIMUM,
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        Scenario(
            metadata = metadata,
            eventReporter = eventReporter,
            experiment = DefaultPreselectLinkExperiment(eventReporter, mode),
            integrationConfiguration = integrationConfiguration,
        ).block()
        eventReporter.validate()
    }

    private data class Scenario(
        val metadata: PaymentMethodMetadata,
        val eventReporter: FakeEventReporter,
        val experiment: PreselectLinkExperiment,
        val integrationConfiguration: PaymentElementLoader.Configuration,
    ) {
        fun getSelection(customer: CustomerState? = null) = experiment.getSelection(
            metadata = metadata,
            customer = customer,
            integrationConfiguration = integrationConfiguration,
        )

        fun assertNoSelectionOrExposure() {
            assertThat(getSelection()).isNull()
            eventReporter.experimentExposureCalls.expectNoEvents()
        }
    }

    private fun linkState(
        loginState: LinkState.LoginState = LinkState.LoginState.LoggedOut,
        configurationBrand: LinkBrand = LinkBrand.Link,
    ) = LinkState(
        configuration = createLinkConfiguration().copy(linkBrand = configurationBrand),
        loginState = loginState,
        signupMode = null,
    )

    private fun metadata(variant: String = "treatment"): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(paymentMethodTypes = listOf("card")),
        isGooglePayReady = false,
        linkState = linkState(),
        experimentsData = ElementsSession.ExperimentsData(
            arbId = "arb_123",
            experimentAssignments = mapOf(ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK to variant),
        ),
    )
}
