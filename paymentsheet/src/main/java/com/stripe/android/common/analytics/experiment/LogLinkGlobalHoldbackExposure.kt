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
            // TODO(carlosmuvi): call lookup to retrieve the returning link user status
            val isReturningLinkUser = false
            // TODO(carlosmuvi): call lookup to retrieve the link native status
            val isLinkNative = false
            val experimentsData = requireNotNull(elementsSession.experimentsData)
            val holdbackOn = elementsSession.linkSettings?.linkGlobalHoldbackOn == true
            eventReporter.onExperimentExposure(
                experiment = LinkGlobalHoldback(
                    arbId = experimentsData.arbId,
                    isReturningLinkUser = isReturningLinkUser,
                    isLinkNative = isLinkNative,
                    group = if (holdbackOn) TREATMENT else CONTROL,
                ),
            )
        }
    }
}
