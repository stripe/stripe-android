package com.stripe.android.common.analytics.experiment

import com.stripe.android.common.analytics.experiment.ExperimentGroup.CONTROL
import com.stripe.android.common.analytics.experiment.ExperimentGroup.TREATMENT
import com.stripe.android.common.analytics.experiment.LoggableExperiment.LinkGlobalHoldback
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

internal class LogLinkGlobalHoldbackExposure @Inject constructor(
    private val eventReporter: EventReporter
) {

    operator fun invoke(
        elementsSession: ElementsSession,
    ) = runCatching {
        if (FeatureFlags.linkGlobalHoldbackExposureEnabled.isEnabled) {
            val experimentsData = requireNotNull(elementsSession.experimentsData)
            val holdbackOn = elementsSession.linkSettings?.linkGlobalHoldbackOn == true
            eventReporter.onExperimentExposure(
                experiment = LinkGlobalHoldback(
                    arbId = experimentsData.arbId,
                    group = if (holdbackOn) TREATMENT else CONTROL,
                ),
            )
        }
    }
}
