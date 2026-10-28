package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon

/** Scoped to a sheet so recalculating button state does not repeat an exposure. */
internal class PrimaryButtonIconExperiment(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
) {
    private val exposedAssignments = mutableSetOf<Pair<String, String>>()

    fun getIcon(metadata: PaymentMethodMetadata?, originalIcon: PrimaryButtonIcon): PrimaryButtonIcon {
        if (mode == EventReporter.Mode.Custom || originalIcon != PrimaryButtonIcon.Lock ||
            metadata?.stripeIntent?.isLiveMode != true
        ) {
            return originalIcon
        }

        val experimentsData = metadata.experimentsData ?: return originalIcon
        val group = experimentsData.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK
        ] ?: return originalIcon
        val icon = when (group) {
            "control", "control_test" -> PrimaryButtonIcon.Lock
            "treatment_no_padlock" -> PrimaryButtonIcon.None
            "treatment_replace_padlock" -> PrimaryButtonIcon.ShieldCheck
            else -> return originalIcon
        }

        if (exposedAssignments.add(experimentsData.arbId to group)) {
            eventReporter.onExperimentExposure(
                LoggableExperiment.OcsMobileRemovePrimaryButtonPadlock(
                    experimentsData = experimentsData,
                    group = group,
                    metadata = metadata,
                    mode = mode,
                )
            )
        }
        return icon
    }
}
