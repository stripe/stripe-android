package com.stripe.android.link.gate

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import javax.inject.Inject

internal class DefaultLinkGate @Inject constructor(
    private val configuration: LinkConfiguration
) : LinkGate {
    override val useNativeLink: Boolean
        get() {
            if (configuration.stripeIntent.isLiveMode) {
                return useAttestationEndpoints
            }
            return FeatureFlags.nativeLinkEnabled.isEnabled
        }

    override val useAttestationEndpoints: Boolean
        get() {
            if (configuration.stripeIntent.isLiveMode) {
                return configuration.useAttestationEndpointsForLink
            }
            return FeatureFlags.nativeLinkAttestationEnabled.isEnabled
        }

    class Factory @Inject constructor() : LinkGate.Factory {
        override fun create(configuration: LinkConfiguration): LinkGate {
            return DefaultLinkGate(configuration)
        }
    }
}
