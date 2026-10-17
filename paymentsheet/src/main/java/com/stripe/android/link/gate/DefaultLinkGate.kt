package com.stripe.android.link.gate

import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import javax.inject.Inject

internal class DefaultLinkGate(
    private val stripeIntent: StripeIntent,
    private val useAttestationEndpointsForLink: Boolean,
    private val suppressLink2faModal: Boolean,
    private val disableRuxInFlowController: Boolean,
) : LinkGate {

    @Inject
    constructor(configuration: LinkConfiguration) : this(
        stripeIntent = configuration.stripeIntent,
        useAttestationEndpointsForLink = configuration.useAttestationEndpointsForLink,
        suppressLink2faModal = configuration.suppress2faModal,
        disableRuxInFlowController = configuration.disableRuxInFlowController,
    )

    constructor(elementsSession: ElementsSession) : this(
        stripeIntent = elementsSession.stripeIntent,
        useAttestationEndpointsForLink = elementsSession.useAttestationEndpointsForLink,
        suppressLink2faModal = elementsSession.suppressLink2faModal,
        disableRuxInFlowController = elementsSession.disableRuxInFlowController,
    )

    override val useNativeLink: Boolean
        get() {
            if (stripeIntent.isLiveMode) {
                return useAttestationEndpoints
            }
            return when (FeatureFlags.nativeLinkEnabled.value) {
                FeatureFlag.Flag.Disabled -> false
                FeatureFlag.Flag.Enabled -> true
                FeatureFlag.Flag.NotSet -> useAttestationEndpoints
            }
        }

    override val useAttestationEndpoints: Boolean
        get() {
            if (stripeIntent.isLiveMode) {
                return useAttestationEndpointsForLink
            }
            return when (FeatureFlags.nativeLinkAttestationEnabled.value) {
                FeatureFlag.Flag.Disabled -> false
                FeatureFlag.Flag.Enabled -> true
                FeatureFlag.Flag.NotSet -> useAttestationEndpointsForLink
            }
        }

    override val suppress2faModal: Boolean
        get() {
            return useNativeLink.not() || suppressLink2faModal
        }

    override val useInlineOtpInWalletButtons: Boolean
        get() = FeatureFlags.showInlineOtpInWalletButtons.isEnabled && useNativeLink

    override val showRuxInFlowController: Boolean
        get() = useNativeLink && !disableRuxInFlowController

    class Factory @Inject constructor() : LinkGate.Factory {
        override fun create(elementsSession: ElementsSession): LinkGate {
            return DefaultLinkGate(elementsSession)
        }

        override fun create(configuration: LinkConfiguration): LinkGate {
            return DefaultLinkGate(configuration)
        }
    }
}
