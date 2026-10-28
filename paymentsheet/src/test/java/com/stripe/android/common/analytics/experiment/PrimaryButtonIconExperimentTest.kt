package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment.OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class PrimaryButtonIconExperimentTest {
    @Test
    fun `applies each variant and exposes it once`(
        @TestParameter variant: Variant,
        @TestParameter mode: PayMode,
    ) = runScenario(mode = mode.mode) {
        val metadata = metadata(variant.group)
        repeat(3) {
            assertThat(experiment.getIcon(metadata, PrimaryButtonIcon.Lock)).isEqualTo(variant.icon)
        }

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
        assertThat(exposure.experiment).isEqualTo(OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK)
        assertThat(exposure.arbId).isEqualTo("arb_123")
        assertThat(exposure.group).isEqualTo(variant.group)
        assertThat(exposure.dimensions).containsEntry("in_app_elements_integration_type", mode.dimension)
        assertThat(exposure.dimensions).containsEntry("displayed_payment_method_types", "card")
        assertThat(exposure.dimensions).containsEntry("displayed_payment_method_types_including_wallets", "card")
    }

    @Test
    fun `testmode keeps original icon without exposure`(@TestParameter variant: Variant) = runScenario {
        val metadata = metadata(variant.group).copy(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(isLiveMode = false),
        )
        assertThat(experiment.getIcon(metadata, PrimaryButtonIcon.Lock)).isEqualTo(PrimaryButtonIcon.Lock)
    }

    @Test
    fun `FlowController keeps original icon without exposure`(@TestParameter variant: Variant) =
        runScenario(mode = EventReporter.Mode.Custom) {
            assertThat(experiment.getIcon(metadata(variant.group), PrimaryButtonIcon.Lock))
                .isEqualTo(PrimaryButtonIcon.Lock)
        }

    @Test
    fun `unlocked buttons keep no icon without exposure`(@TestParameter variant: Variant) = runScenario {
        assertThat(experiment.getIcon(metadata(variant.group), PrimaryButtonIcon.None))
            .isEqualTo(PrimaryButtonIcon.None)
    }

    @Test
    fun `missing metadata keeps original icon without exposure`() = runScenario {
        assertThat(experiment.getIcon(null, PrimaryButtonIcon.Lock)).isEqualTo(PrimaryButtonIcon.Lock)
    }

    @Test
    fun `missing experiments data keeps original icon without exposure`() = runScenario {
        assertThat(experiment.getIcon(metadata("control").copy(experimentsData = null), PrimaryButtonIcon.Lock))
            .isEqualTo(PrimaryButtonIcon.Lock)
    }

    @Test
    fun `missing assignment keeps original icon without exposure`() = runScenario {
        val metadata = metadata("control").copy(
            experimentsData = ElementsSession.ExperimentsData("arb_123", emptyMap()),
        )
        assertThat(experiment.getIcon(metadata, PrimaryButtonIcon.Lock)).isEqualTo(PrimaryButtonIcon.Lock)
    }

    @Test
    fun `unknown assignment keeps original icon without exposure`() = runScenario {
        assertThat(experiment.getIcon(metadata("future_variant"), PrimaryButtonIcon.Lock))
            .isEqualTo(PrimaryButtonIcon.Lock)
    }

    @Test
    fun `setup intent in complete mode also uses assignment`() = runScenario {
        val metadata = metadata("treatment_no_padlock").copy(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(isLiveMode = true),
        )
        assertThat(experiment.getIcon(metadata, PrimaryButtonIcon.Lock)).isEqualTo(PrimaryButtonIcon.None)
        eventReporter.experimentExposureCalls.awaitItem()
    }

    @Test
    fun `ineligible state does not prevent later eligible exposure`() = runScenario {
        val metadata = metadata("treatment_no_padlock")
        experiment.getIcon(metadata, PrimaryButtonIcon.None)
        eventReporter.experimentExposureCalls.expectNoEvents()

        assertThat(experiment.getIcon(metadata, PrimaryButtonIcon.Lock)).isEqualTo(PrimaryButtonIcon.None)
        eventReporter.experimentExposureCalls.awaitItem()
    }

    private fun runScenario(
        mode: EventReporter.Mode = EventReporter.Mode.Complete,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        Scenario(PrimaryButtonIconExperiment(eventReporter, mode), eventReporter).block()
        eventReporter.validate()
    }

    private data class Scenario(
        val experiment: PrimaryButtonIconExperiment,
        val eventReporter: FakeEventReporter,
    )

    internal enum class Variant(val group: String, val icon: PrimaryButtonIcon) {
        Control("control", PrimaryButtonIcon.Lock),
        ControlTest("control_test", PrimaryButtonIcon.Lock),
        NoPadlock("treatment_no_padlock", PrimaryButtonIcon.None),
    }

    internal enum class PayMode(val mode: EventReporter.Mode, val dimension: String) {
        PaymentSheet(EventReporter.Mode.Complete, "paymentsheet"),
        Embedded(EventReporter.Mode.Embedded, "embedded"),
    }

    private fun metadata(group: String): PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            isLiveMode = true,
            paymentMethodTypes = listOf("card"),
        ),
        experimentsData = ElementsSession.ExperimentsData(
            arbId = "arb_123",
            experimentAssignments = mapOf(OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK to group),
        ),
    )
}
