package com.stripe.android.link.domain

import com.stripe.android.core.Logger
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.paymentsheet.state.LinkState
import javax.inject.Inject

internal class LinkProminenceFeatureProvider @Inject constructor(
    private val linkGateFactory: LinkGate.Factory,
    private val logger: Logger,
) {

    /**
     * Returns true if the Link prominence feature should be shown in the flow controller.
     */
    fun showVerificationOnFlowControllerLinkSelection(
        linkState: LinkState,
    ): Boolean {
        val linkConfiguration = linkState.configuration

        if (!FeatureFlags.linkProminenceInFlowController.isEnabled) {
            logger.debug("Prominence disabled: Client side feature flag is disabled")
            return false
        }

        if (linkConfiguration.suppress2faModal == true) {
            logger.debug("Prominence disabled: Backend kill-switch is enabled")
            return false
        }

        if (linkState.loginState != LinkState.LoginState.NeedsVerification) {
            logger.debug("Prominence disabled: No returning link customer available")
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
