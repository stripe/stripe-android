package com.stripe.android.link.domain

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.LinkHandler
import javax.inject.Inject

/**
 * This class is responsible for determining which link prominence features are enabled.
 */
internal class GetLinkProminenceFeatures @Inject constructor(
    private val linkHandler: LinkHandler
) {

    operator fun invoke(): List<LinkProminenceFeature> {
        return listOfNotNull(
            eager2FAInFlowController()
        )
    }

    private fun eager2FAInFlowController(): LinkProminenceFeature? {
        val linkConfiguration = linkHandler.linkConfiguration.value
        val suppress2faModal = linkConfiguration?.suppress2faModal == true
        return if (FeatureFlags.linkProminenceInFlowController.isEnabled && suppress2faModal.not()) {
            LinkProminenceFeature.Eager2FAInFlowController
        } else {
            null
        }
    }
}

enum class LinkProminenceFeature {
    Eager2FAInFlowController
}