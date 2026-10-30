package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.PrimaryButtonIcon

internal object PrimaryButtonExperiment {
    // Only call when displaying a confirmation button that would otherwise show a padlock.
    fun getIcon(
        metadata: PaymentMethodMetadata,
        eventReporter: EventReporter,
        mode: EventReporter.Mode,
    ): PrimaryButtonIcon {
        if (!metadata.stripeIntent.isLiveMode || mode == EventReporter.Mode.Custom) {
            return PrimaryButtonIcon.Lock
        }

        val experimentsData = metadata.experimentsData ?: return PrimaryButtonIcon.Lock
        val variant = experimentsData.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_REMOVE_PRIMARY_BUTTON_PADLOCK
        ]
        val icon = when (variant) {
            "control", "control_test" -> PrimaryButtonIcon.Lock
            "treatment_no_padlock" -> PrimaryButtonIcon.None
            // Do not log exposures for variants whose behavior is not implemented yet.
            else -> return PrimaryButtonIcon.Lock
        }

        eventReporter.onExperimentExposure(
            LoggableExperiment.OcsMobileRemovePrimaryButtonPadlock(
                experimentsData = experimentsData,
                group = variant,
                metadata = metadata,
                mode = mode,
            )
        )
        return icon
    }
}
