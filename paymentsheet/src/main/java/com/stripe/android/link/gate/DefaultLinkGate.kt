package com.stripe.android.link.gate

import com.stripe.android.core.utils.FeatureFlag
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import javax.inject.Inject

internal class DefaultLinkGate @Inject constructor(
    private val configuration: LinkConfiguration
) : LinkGate {

    override val useNativeLink: Boolean
        get() {
            return configuration.useAttestationEndpointsForLink
//            if (configuration.stripeIntent.isLiveMode) {
//                return configuration.useAttestationEndpointsForLink
//            }
//            return when (FeatureFlags.nativeLinkEnabled.value) {
//                FeatureFlag.Flag.Disabled -> false
//                FeatureFlag.Flag.Enabled, FeatureFlag.Flag.NotSet -> true
//            }
        }

    override val useAttestationEndpoints: Boolean
        get() {
            return configuration.useAttestationEndpointsForLink
        }

    override val suppress2faModal: Boolean
        get() {
            return useNativeLink.not() || configuration.suppress2faModal
        }

    class Factory @Inject constructor() : LinkGate.Factory {
        override fun create(configuration: LinkConfiguration): LinkGate {
            return DefaultLinkGate(configuration)
        }
    }
}
