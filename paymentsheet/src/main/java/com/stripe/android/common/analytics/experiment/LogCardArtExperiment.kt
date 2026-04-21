package com.stripe.android.common.analytics.experiment

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

internal interface LogCardArtExperiment {
    /**
     * Logs the card art experiment exposure and returns whether card art should be enabled.
     *
     * Card art is enabled when:
     * - The experiment variant is "treatment", OR
     * - The local feature flag is enabled (debug-only fallback for testing)
     */
    operator fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Boolean
}

internal class DefaultLogCardArtExperiment @Inject constructor(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
) : LogCardArtExperiment {

    override fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Boolean {
        val experimentsData = elementsSession.experimentsData
            ?: return FeatureFlags.enableCardArt.isEnabled

        val variant = experimentsData.experimentAssignments[ExperimentAssignment.OCS_MOBILE_CARD_ART]
            ?: return FeatureFlags.enableCardArt.isEnabled

        eventReporter.onExperimentExposure(
            LoggableExperiment.OcsMobileCardArt(
                experimentsData = experimentsData,
                experiment = ExperimentAssignment.OCS_MOBILE_CARD_ART,
                group = variant,
                paymentMethodMetadata = paymentMethodMetadata,
                mode = mode,
            )
        )

        return variant == TREATMENT || FeatureFlags.enableCardArt.isEnabled
    }

    private companion object {
        const val TREATMENT = "treatment"
    }
}
