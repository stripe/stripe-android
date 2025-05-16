package com.stripe.android.link

import com.stripe.android.link.domain.LinkProminenceFeatureProvider

internal class FakeLinkProminenceFeatureProvider : LinkProminenceFeatureProvider {

    override fun shouldShowEarlyVerificationInFlowController(linkConfiguration: LinkConfiguration): Boolean {
        return false
    }
}
