package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
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

internal class PrimaryButtonExperimentTest {
    @Test
    fun `control keeps padlock and logs exposure`() = assertVariantIconAndExposure(
        variant = "control",
        expectedIcon = PrimaryButtonIcon.Lock,
    )

    @Test
    fun `control_test keeps padlock and logs exposure`() = assertVariantIconAndExposure(
        variant = "control_test",
        expectedIcon = PrimaryButtonIcon.Lock,
    )

    @Test
    fun `treatment_no_padlock removes padlock and logs exposure`() = assertVariantIconAndExposure(
        variant = "treatment_no_padlock",
        expectedIcon = PrimaryButtonIcon.None,
    )

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
    fun `treatment_replace_padlock keeps padlock without logging exposure`() = assertUnsupportedVariant(
        variant = "treatment_replace_padlock",
    )

    @Test
    fun `unknown variant keeps padlock without logging exposure`() = assertUnsupportedVariant(variant = "unknown")

    @Test
    fun `empty variant keeps padlock without logging exposure`() = assertUnsupportedVariant(variant = "")

    private fun assertVariantIconAndExposure(
        variant: String,
        expectedIcon: PrimaryButtonIcon,
    ) = runScenario(metadata = metadata(variant)) {
        assertThat(getIcon()).isEqualTo(expectedIcon)

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
        assertThat(exposure.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK)
        assertThat(exposure.arbId).isEqualTo("arb_123")
        assertThat(exposure.group).isEqualTo(variant)
        assertThat(exposure.dimensions).containsExactly(
            "displayed_payment_method_types", "card",
            "displayed_payment_method_types_including_wallets", "card,google_pay",
            "in_app_elements_integration_type", "embedded",
        )
    }

    private fun assertUnsupportedVariant(variant: String) = runScenario(metadata = metadata(variant)) {
        assertThat(getIcon()).isEqualTo(PrimaryButtonIcon.Lock)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = metadata(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()
        Scenario(metadata, eventReporter).block()
        eventReporter.validate()
    }

    private class Scenario(
        val metadata: PaymentMethodMetadata,
        val eventReporter: FakeEventReporter,
    ) {
        fun getIcon(): PrimaryButtonIcon = PrimaryButtonExperiment.getIcon(
            metadata = metadata,
            eventReporter = eventReporter,
            mode = EventReporter.Mode.Embedded,
        )
    }

    private fun metadata(variant: String = "treatment_no_padlock"): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
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
