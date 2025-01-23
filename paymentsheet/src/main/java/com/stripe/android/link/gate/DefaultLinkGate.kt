package com.stripe.android.link.gate

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import javax.inject.Inject

internal class DefaultLinkGate @Inject constructor(
    private val configuration: LinkConfiguration
) : LinkGate {
    override val useNativeLink: Boolean
        get() {
            if (FeatureFlags.nativeLinkEnabled.isEnabled) return true
            return useAttestationEndpoints
        }

    override val useAttestationEndpoints: Boolean
        get() {
            if (FeatureFlags.nativeLinkAttestationEnabled.isEnabled) return true
            return configuration.useAttestationEndpointsForLink
        }

    class Factory @Inject constructor() : LinkGate.Factory {
        override fun create(configuration: LinkConfiguration): LinkGate {
            return DefaultLinkGate(configuration)
        }
    }
}
