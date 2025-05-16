package com.stripe.android.link.domain

import com.stripe.android.core.Logger
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.gate.LinkGate
import javax.inject.Inject

/**
 * Provider interface for Link feature prominence settings and behaviors, handling
 * the logic for determining when to show special Link UI elements or trigger automatic
 * behaviors to enhance the Link payment experience.
 */
internal interface LinkProminenceFeatureProvider {

    /**
     * In FlowController, this method determines if the 2FA
     * dialog should be shown eagerly if the user continues with Link.
     */
    fun shouldShowEarlyVerificationInFlowController(linkConfiguration: LinkConfiguration): Boolean
}

internal class DefaultLinkProminenceFeatureProvider @Inject constructor(
    private val linkGateFactory: LinkGate.Factory,
    private val logger: Logger
) : LinkProminenceFeatureProvider {

    override fun shouldShowEarlyVerificationInFlowController(
        linkConfiguration: LinkConfiguration,
    ): Boolean {

        if (!FeatureFlags.linkProminenceInFlowController.isEnabled) {
            logger.debug("Prominence disabled: Client side feature flag is disabled")
            return false
        }

        if (linkConfiguration.suppress2faModal == true) {
            logger.debug("Prominence disabled: Backend kill-switch is enabled")
            return false
        }

        if (!linkGateFactory.create(linkConfiguration).useNativeLink) {
            logger.debug("Prominence disabled: Link native is not enabled")
            return false
        }

        logger.debug("Prominence enabled")
        return true
    }
}
