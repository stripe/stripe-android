package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.CustomerState
import javax.inject.Inject

internal class PreselectLinkExperiment @Inject constructor(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
) {
    // Call once at load time, before computing the initial selection for FlowController or Embedded.
    fun isEnabled(metadata: PaymentMethodMetadata, customer: CustomerState?): Boolean {
        if (mode == EventReporter.Mode.Complete || !metadata.stripeIntent.isLiveMode) return false

        val experimentsData = metadata.experimentsData ?: return false
        val variant = experimentsData.experimentAssignments[ExperimentAssignment.OCS_MOBILE_PRESELECT_LINK]
        val isEnabled = when (variant) {
            "control", "control_test" -> false
            "treatment" -> true
            // Preserve existing behavior and do not log unimplemented variants.
            else -> return false
        }

        eventReporter.onExperimentExposure(
            experiment = LoggableExperiment.OcsMobilePreselectLink(
                experimentsData = experimentsData,
                group = variant,
                metadata = metadata,
                mode = mode,
                hasSavedPaymentMethod = !customer?.paymentMethods.isNullOrEmpty(),
            ),
            paymentMethodMetadata = metadata,
        )
        return isEnabled
    }
}
