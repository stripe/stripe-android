package com.stripe.android.common.analytics.experiment

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

internal interface LogCardArtExperiment {
    operator fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
    )
}

internal class DefaultLogCardArtExperiment @Inject constructor(
    private val eventReporter: EventReporter,
    private val mode: EventReporter.Mode,
) : LogCardArtExperiment {

    override fun invoke(
        elementsSession: ElementsSession,
        paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        val experimentsData = elementsSession.experimentsData ?: return

        experimentsData.experimentAssignments[
            ExperimentAssignment.OCS_MOBILE_CARD_ART,
        ]?.let { variant ->
            eventReporter.onExperimentExposure(
                LoggableExperiment.OcsMobileCardArt(
                    experimentsData = experimentsData,
                    experiment = ExperimentAssignment.OCS_MOBILE_CARD_ART,
                    group = variant,
                    paymentMethodMetadata = paymentMethodMetadata,
                    mode = mode,
                )
            )
        }
    }
}
