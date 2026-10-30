package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PrimaryButtonExperimentTest {
    @Test
    fun `supported variants return icon and log exposure`(
        @TestParameter(value = ["control", "control_test", "treatment_no_padlock"]) variant: String,
        @TestParameter(value = ["Complete", "Embedded"]) mode: EventReporter.Mode,
    ) = runScenario(metadata = metadata(variant), mode = mode) {
        assertThat(getIcon()).isEqualTo(
            if (variant == "treatment_no_padlock") PrimaryButtonIcon.None else PrimaryButtonIcon.Lock
        )

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
        assertThat(exposure.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK)
        assertThat(exposure.arbId).isEqualTo("arb_123")
        assertThat(exposure.group).isEqualTo(variant)
        assertThat(exposure.dimensions).containsExactly(
            "displayed_payment_method_types", "card",
            "displayed_payment_method_types_including_wallets", "card,google_pay",
            "in_app_elements_integration_type", if (mode == EventReporter.Mode.Complete) "paymentsheet" else "embedded",
        )
    }

    @Test
    fun `testmode keeps padlock without logging exposure`(
        @TestParameter(value = ["control", "control_test", "treatment_no_padlock"]) variant: String,
    ) = runScenario(
        metadata = metadata(variant).copy(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(isLiveMode = false),
        ),
    ) {
        assertThat(getIcon()).isEqualTo(PrimaryButtonIcon.Lock)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `FlowController keeps padlock without logging exposure`() = runScenario(mode = EventReporter.Mode.Custom) {
        assertThat(getIcon()).isEqualTo(PrimaryButtonIcon.Lock)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `missing experiments data keeps padlock without logging exposure`() = runScenario(
        metadata = metadata().copy(experimentsData = null),
    ) {
        assertThat(getIcon()).isEqualTo(PrimaryButtonIcon.Lock)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `missing assignment keeps padlock without logging exposure`() = runScenario(
        metadata = metadata().copy(
            experimentsData = ElementsSession.ExperimentsData("arb_123", emptyMap()),
        ),
    ) {
        assertThat(getIcon()).isEqualTo(PrimaryButtonIcon.Lock)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `unsupported variant keeps padlock without logging exposure`(
        @TestParameter(value = ["treatment_replace_padlock", "unknown", ""]) variant: String,
    ) = runScenario(metadata = metadata(variant)) {
        assertThat(getIcon()).isEqualTo(PrimaryButtonIcon.Lock)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = metadata(),
        mode: EventReporter.Mode = EventReporter.Mode.Embedded,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        Scenario(metadata, eventReporter, mode).block()
        eventReporter.validate()
    }

    private class Scenario(
        val metadata: PaymentMethodMetadata,
        val eventReporter: FakeEventReporter,
        val mode: EventReporter.Mode,
    ) {
        fun getIcon(): PrimaryButtonIcon = PrimaryButtonExperiment.getIcon(metadata, eventReporter, mode)
    }

    private fun metadata(variant: String = "treatment_no_padlock"): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                isLiveMode = true,
                paymentMethodTypes = listOf("card"),
            ),
            isGooglePayReady = true,
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "arb_123",
                experimentAssignments = mapOf(ExperimentAssignment.OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK to variant),
            ),
        )
    }
}
