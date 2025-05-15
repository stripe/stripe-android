package com.stripe.android.link.domain

import com.stripe.android.core.Logger
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.paymentsheet.state.LinkState
import javax.inject.Inject

internal interface LinkProminenceFeatureProvider {

    /**
     * On FlowController, upon showing the payment method list, if the user has selects Link, this
     * method will determine if the 2FA dialog should be shown eagerly and confirmed as selection upon successful
     * authentication.
     */
    fun show2FADialogOnLinkSelectedInFlowController(linkState: LinkState): Boolean
}

internal class DefaultLinkProminenceFeatureProvider @Inject constructor(
    private val linkGateFactory: LinkGate.Factory,
    private val logger: Logger,
) : LinkProminenceFeatureProvider {

    override fun show2FADialogOnLinkSelectedInFlowController(
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
