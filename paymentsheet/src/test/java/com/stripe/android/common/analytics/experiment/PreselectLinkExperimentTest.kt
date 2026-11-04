package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
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
        assertThat(experiment.isEnabled(metadata, customer = null)).isEqualTo(variant == "treatment")

        val call = eventReporter.experimentExposureCalls.awaitItem()
        assertThat(call.paymentMethodMetadata).isEqualTo(metadata)
        val exposure = call.experiment
        assertThat(exposure.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK)
        assertThat(exposure.arbId).isEqualTo("arb_123")
        assertThat(exposure.group).isEqualTo(variant)
        assertThat(exposure.dimensions).containsExactly(
            "displayed_payment_method_types", "card",
            "displayed_payment_method_types_including_wallets", "card",
            "in_app_elements_integration_type", if (mode == EventReporter.Mode.Custom) "flowcontroller" else "embedded",
            "link_enabled", "false",
            "google_pay_enabled", "false",
            "has_saved_payment_method", "false",
        )
    }

    @Test
    fun `unsupported variants do not change behavior or log exposure`(
        @TestParameter("unknown", "") variant: String,
    ) = runScenario(metadata = metadata(variant)) {
        assertThat(experiment.isEnabled(metadata, customer = null)).isFalse()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `missing experiments data does not log exposure`() = runScenario(
        metadata = metadata().copy(experimentsData = null),
    ) {
        assertThat(experiment.isEnabled(metadata, customer = null)).isFalse()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `missing assignment does not log exposure`() = runScenario(
        metadata = metadata().copy(experimentsData = ElementsSession.ExperimentsData("arb_123", emptyMap())),
    ) {
        assertThat(experiment.isEnabled(metadata, customer = null)).isFalse()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `PaymentSheet does not change behavior or log exposure`() = runScenario(mode = EventReporter.Mode.Complete) {
        assertThat(experiment.isEnabled(metadata, customer = null)).isFalse()
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `test mode does not change behavior or log exposure`() = runScenario(
        metadata = metadata().copy(stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD),
    ) {
        assertThat(metadata.stripeIntent.isLiveMode).isFalse()
        assertThat(experiment.isEnabled(metadata, customer = null)).isFalse()
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
        assertThat(experiment.isEnabled(metadata, customer)).isTrue()

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
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        Scenario(metadata, eventReporter, PreselectLinkExperiment(eventReporter, mode)).block()
        eventReporter.validate()
    }

    private data class Scenario(
        val metadata: PaymentMethodMetadata,
        val eventReporter: FakeEventReporter,
        val experiment: PreselectLinkExperiment,
    )

    private fun metadata(variant: String = "treatment"): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card"),
            isLiveMode = true,
        ),
        isGooglePayReady = false,
        experimentsData = ElementsSession.ExperimentsData(
            arbId = "arb_123",
            experimentAssignments = mapOf(ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK to variant),
        ),
    )
}
