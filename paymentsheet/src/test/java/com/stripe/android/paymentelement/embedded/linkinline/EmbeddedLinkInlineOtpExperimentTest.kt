package com.stripe.android.paymentelement.embedded.linkinline

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.ElementsSession.ExperimentsData
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineOtpExperiment.Group
import org.junit.Test

internal class EmbeddedLinkInlineOtpExperimentTest {

    @Test
    fun `missing experiments data shows the card without exposure`() {
        val experiment = EmbeddedLinkInlineOtpExperiment.from(experimentsData = null)

        assertThat(experiment).isEqualTo(EmbeddedLinkInlineOtpExperiment.Unassigned)
        assertThat(experiment.showsCard).isTrue()
    }

    @Test
    fun `missing assignment shows the card without exposure`() {
        val experiment = EmbeddedLinkInlineOtpExperiment.from(
            experimentsData = experimentsData(
                ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK to "treatment",
            ),
        )

        assertThat(experiment).isEqualTo(EmbeddedLinkInlineOtpExperiment.Unassigned)
        assertThat(experiment.showsCard).isTrue()
    }

    @Test
    fun `treatment shows the card`() {
        val experiment = assigned("treatment")

        assertThat(experiment.group).isEqualTo(Group.Treatment)
        assertThat(experiment.showsCard).isTrue()
    }

    @Test
    fun `control keeps the regular Link row`() {
        val experiment = assigned("control")

        assertThat(experiment.group).isEqualTo(Group.Control)
        assertThat(experiment.showsCard).isFalse()
    }

    @Test
    fun `control_test keeps the regular Link row`() {
        val experiment = assigned("control_test")

        assertThat(experiment.group).isEqualTo(Group.ControlTest)
        assertThat(experiment.showsCard).isFalse()
    }

    @Test
    fun `unknown group keeps the regular Link row without exposure`() {
        val experiment = EmbeddedLinkInlineOtpExperiment.from(
            experimentsData = experimentsData(
                ExperimentAssignment.OCS_MOBILE_EMBEDDED_LINK_INLINE_OTP to "treatment_v2",
            ),
        )

        assertThat(experiment).isEqualTo(EmbeddedLinkInlineOtpExperiment.UnknownGroup)
        assertThat(experiment.showsCard).isFalse()
    }

    @Test
    fun `assignment keeps the experiments data for exposure`() {
        val experimentsData = experimentsData(
            ExperimentAssignment.OCS_MOBILE_EMBEDDED_LINK_INLINE_OTP to "control",
        )

        val experiment = EmbeddedLinkInlineOtpExperiment.from(experimentsData)

        assertThat(experiment).isEqualTo(
            EmbeddedLinkInlineOtpExperiment.Assigned(experimentsData = experimentsData, group = Group.Control)
        )
    }

    private fun assigned(group: String): EmbeddedLinkInlineOtpExperiment.Assigned {
        val experiment = EmbeddedLinkInlineOtpExperiment.from(
            experimentsData = experimentsData(ExperimentAssignment.OCS_MOBILE_EMBEDDED_LINK_INLINE_OTP to group),
        )
        assertThat(experiment).isInstanceOf(EmbeddedLinkInlineOtpExperiment.Assigned::class.java)
        return experiment as EmbeddedLinkInlineOtpExperiment.Assigned
    }

    private fun experimentsData(vararg assignments: Pair<ExperimentAssignment, String>) = ExperimentsData(
        arbId = "arb_123",
        experimentAssignments = assignments.toMap(),
    )
}
