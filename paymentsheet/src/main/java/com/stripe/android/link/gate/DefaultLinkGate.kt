package com.stripe.android.link.gate

import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.RequestSurface
import javax.inject.Inject

internal class DefaultLinkGate(
    private val stripeIntent: StripeIntent,
    private val useAttestationEndpointsForLink: Boolean,
    private val suppressLink2faModal: Boolean,
    private val disableRuxInFlowController: Boolean,
    private val requestSurface: RequestSurface,
) : LinkGate {

    @Inject
    constructor(configuration: LinkConfiguration, requestSurface: RequestSurface) : this(
        stripeIntent = configuration.stripeIntent,
        useAttestationEndpointsForLink = configuration.useAttestationEndpointsForLink,
        suppressLink2faModal = configuration.suppress2faModal,
        disableRuxInFlowController = configuration.disableRuxInFlowController,
        requestSurface = requestSurface,
    )

    constructor(elementsSession: ElementsSession, requestSurface: RequestSurface) : this(
        stripeIntent = elementsSession.stripeIntent,
        useAttestationEndpointsForLink = elementsSession.useAttestationEndpointsForLink,
        suppressLink2faModal = elementsSession.suppressLink2faModal,
        disableRuxInFlowController = elementsSession.disableRuxInFlowController,
        requestSurface = requestSurface,
    )

    /**
     * Link limits the session keys it issues for the Identity surface to Identity's own use, so a key read out of the
     * app can't act on the rest of the account. Identity therefore offers native Link to merchants that haven't set up
     * app attestation; merchants that have keep attesting.
     * #TODO - Networked Identity [NI-Contract]: Link's backend doesn't limit these keys yet.
     */
    private val allowsNativeLinkWithoutAttestation: Boolean
        get() = requestSurface == RequestSurface.IdentityProduct

    override val useNativeLink: Boolean
        get() {
            if (allowsNativeLinkWithoutAttestation) return true
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

    class Factory @Inject constructor(
        private val requestSurface: RequestSurface,
    ) : LinkGate.Factory {
        override fun create(elementsSession: ElementsSession): LinkGate {
            return DefaultLinkGate(elementsSession, requestSurface)
        }

        override fun create(configuration: LinkConfiguration): LinkGate {
            return DefaultLinkGate(configuration, requestSurface)
        }
    }
}
