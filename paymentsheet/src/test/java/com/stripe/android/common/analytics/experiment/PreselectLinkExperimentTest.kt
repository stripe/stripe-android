package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PreselectLinkExperimentTest {
    @Test
    fun `supported variants log one exposure in eligible integrations`(
        @TestParameter("control", "control_test", "treatment") variant: String,
        @TestParameter("Custom", "Embedded") mode: EventReporter.Mode,
    ) = runScenario(metadata = metadata(variant), mode = mode) {
        assertThat(getSelection()).isEqualTo(
            PaymentSelection.Link(brand = metadata.linkState!!.configuration.linkBrand)
                .takeIf { variant == "treatment" },
        )

        val call = eventReporter.experimentExposureCalls.awaitItem()
        assertThat(call.paymentMethodMetadata).isEqualTo(metadata)
        val exposure = call.experiment
        assertThat(exposure.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK)
        assertThat(exposure.arbId).isEqualTo("arb_123")
        assertThat(exposure.group).isEqualTo(variant)
        assertThat(exposure.dimensions).containsExactly(
            "displayed_payment_method_types", "card",
            "displayed_payment_method_types_including_wallets", "card,link",
            "in_app_elements_integration_type", if (mode == EventReporter.Mode.Custom) "flowcontroller" else "embedded",
            "link_enabled", "true",
            "link_login_state", "logged_out",
            "google_pay_enabled", "false",
            "has_saved_payment_method", "false",
        )
    }

    @Test
    fun `unsupported variants do not change behavior or log exposure`(
        @TestParameter("unknown", "") variant: String,
    ) = runScenario(metadata = metadata(variant)) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `missing experiments data does not log exposure`() = runScenario(
        metadata = metadata().copy(experimentsData = null),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `missing assignment does not log exposure`() = runScenario(
        metadata = metadata().copy(experimentsData = ElementsSession.ExperimentsData("arb_123", emptyMap())),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `PaymentSheet does not change behavior or log exposure`() = runScenario(mode = EventReporter.Mode.Complete) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `test mode does not change behavior or log exposure`() = runScenario(
        metadata = metadata().copy(stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD),
    ) {
        assertThat(metadata.stripeIntent.isLiveMode).isFalse()
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `unavailable Link does not change behavior or log exposure`() = runScenario(
        metadata = metadata().copy(linkStateResult = null),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `WalletButtonHidden excludes all login states and variants`(
        @TestParameter loginState: LinkState.LoginState,
        @TestParameter("control", "control_test", "treatment") variant: String,
        @TestParameter("Custom", "Embedded") mode: EventReporter.Mode,
    ) = runScenario(
        mode = mode,
        metadata = metadata(variant).copy(
            linkStateResult = LinkState(createLinkConfiguration(), loginState, signupMode = null),
        ),
        integrationConfiguration = integrationConfiguration(
            mode,
            PaymentSheet.LinkConfiguration.Display.WalletButtonHidden,
        ),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `Never display does not change behavior or log exposure`() = runScenario(
        integrationConfiguration = integrationConfiguration(
            EventReporter.Mode.Custom,
            PaymentSheet.LinkConfiguration.Display.Never,
        ),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @OptIn(WalletButtonsPreview::class)
    @Test
    fun `external wallet buttons do not change behavior or log exposure`(
        @TestParameter("control", "control_test", "treatment") variant: String,
    ) = runScenario(
        metadata = metadata(variant),
        integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(
            PaymentSheet.Configuration.Builder("Merchant")
                .walletButtons(PaymentSheet.WalletButtonsConfiguration(willDisplayExternally = true))
                .build(),
        ),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `ExpressCheckoutElement does not change behavior or log exposure`() = runScenario(
        integrationConfiguration = PaymentElementLoader.Configuration.ExpressCheckoutElement(
            PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
        ),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `checkout initialization does not change behavior or log exposure`() = runScenario(
        mode = EventReporter.Mode.Embedded,
        initializationMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = "instances_123",
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(liveMode = true),
            collectedEmail = null,
            configuredElements = emptyList(),
        ),
    ) {
        assertThat(getSelection()).isNull()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `exposure includes Link login state and payment method availability`(
        @TestParameter loginState: LinkState.LoginState,
        @TestParameter hasSavedPaymentMethod: Boolean,
        @TestParameter isGooglePayReady: Boolean,
    ) = runScenario(
        metadata = metadata().copy(
            linkStateResult = LinkState(
                configuration = createLinkConfiguration(),
                loginState = loginState,
                signupMode = null,
            ),
            isGooglePayReady = isGooglePayReady,
        ),
    ) {
        val customer = CustomerState(
            paymentMethods = if (hasSavedPaymentMethod) {
                listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            } else {
                emptyList()
            },
            defaultPaymentMethodId = null,
        )
        assertThat(getSelection(customer)).isEqualTo(
            PaymentSelection.Link(metadata.linkState!!.configuration.linkBrand),
        )

        val dimensions = eventReporter.experimentExposureCalls.awaitItem().experiment.dimensions
        assertThat(dimensions).containsEntry("link_enabled", "true")
        assertThat(dimensions).containsEntry("google_pay_enabled", isGooglePayReady.toString())
        assertThat(dimensions).containsEntry("has_saved_payment_method", hasSavedPaymentMethod.toString())
        assertThat(dimensions).containsEntry(
            "link_login_state",
            when (loginState) {
                LinkState.LoginState.LoggedIn -> "logged_in"
                LinkState.LoginState.NeedsVerification -> "needs_verification"
                LinkState.LoginState.NeedsWebVerification -> "needs_web_verification"
                LinkState.LoginState.LoggedOut -> "logged_out"
            },
        )
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = metadata(),
        mode: EventReporter.Mode = EventReporter.Mode.Custom,
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PaymentSheetFixtures.PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
        integrationConfiguration: PaymentElementLoader.Configuration = integrationConfiguration(mode),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        Scenario(
            metadata, eventReporter, PreselectLinkExperiment(eventReporter, mode),
            initializationMode, integrationConfiguration,
        ).block()
        eventReporter.validate()
    }

    private data class Scenario(
        val metadata: PaymentMethodMetadata,
        val eventReporter: FakeEventReporter,
        val experiment: PreselectLinkExperiment,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val integrationConfiguration: PaymentElementLoader.Configuration,
    ) {
        fun getSelection(customer: CustomerState? = null) = experiment.getSelection(
            metadata = metadata,
            customer = customer,
            initializationMode = initializationMode,
            integrationConfiguration = integrationConfiguration,
        )
    }

    private fun integrationConfiguration(
        mode: EventReporter.Mode,
        linkDisplay: PaymentSheet.LinkConfiguration.Display = PaymentSheet.LinkConfiguration.Display.Automatic,
    ): PaymentElementLoader.Configuration {
        val link = PaymentSheet.LinkConfiguration(display = linkDisplay)
        return if (mode == EventReporter.Mode.Embedded) {
            PaymentElementLoader.Configuration.Embedded(
                isRowSelectionImmediateAction = false,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant").link(link).build(),
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            )
        } else {
            PaymentElementLoader.Configuration.PaymentSheet(
                PaymentSheet.Configuration.Builder("Merchant").link(link).build(),
            )
        }
    }

    private fun metadata(variant: String = "treatment"): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card"),
            isLiveMode = true,
        ),
        isGooglePayReady = false,
        linkState = LinkState(
            configuration = createLinkConfiguration(),
            loginState = LinkState.LoginState.LoggedOut,
            signupMode = null,
        ),
        experimentsData = ElementsSession.ExperimentsData(
            arbId = "arb_123",
            experimentAssignments = mapOf(ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK to variant),
        ),
    )
}
