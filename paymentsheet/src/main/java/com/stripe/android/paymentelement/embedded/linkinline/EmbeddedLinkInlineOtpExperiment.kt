package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment

/**
 * The customer's assignment in the `ocs_mobile_embedded_link_inline_otp` experiment.
 */
internal sealed interface EmbeddedLinkInlineOtpExperiment {
    /**
     * Whether the customer sees the inline verification card.
     */
    val showsCard: Boolean

    /**
     * No assignment, e.g. in testmode or a rollout without an experiment. The card shows and no exposure is logged.
     */
    data object Unassigned : EmbeddedLinkInlineOtpExperiment {
        override val showsCard: Boolean = true
    }

    /**
     * A group this version doesn't implement. The regular Link row shows and no exposure is logged.
     */
    data object UnknownGroup : EmbeddedLinkInlineOtpExperiment {
        override val showsCard: Boolean = false
    }

    /**
     * A group whose exposure is logged, at the same moment in every group.
     */
    data class Assigned(
        val experimentsData: ElementsSession.ExperimentsData,
        val group: Group,
    ) : EmbeddedLinkInlineOtpExperiment {
        override val showsCard: Boolean
            get() = group == Group.Treatment
    }

    enum class Group(val value: String) {
        Treatment("treatment"),
        Control("control"),
        ControlTest("control_test"),
    }

    companion object {
        fun from(experimentsData: ElementsSession.ExperimentsData?): EmbeddedLinkInlineOtpExperiment {
            val value = experimentsData?.experimentAssignments?.get(
                ExperimentAssignment.OCS_MOBILE_EMBEDDED_LINK_INLINE_OTP
            ) ?: return Unassigned
            val group = Group.entries.firstOrNull { it.value == value } ?: return UnknownGroup
            return Assigned(experimentsData = experimentsData, group = group)
        }
    }
}
