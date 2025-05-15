package com.stripe.android.link

import com.stripe.android.link.domain.LinkProminenceFeatureProvider

internal class FakeLinkProminenceFeatureProvider : LinkProminenceFeatureProvider {

    var shouldShowEarlyVerificationInFlowController: Boolean = false

    override fun shouldShowEarlyVerificationInFlowController(linkConfiguration: LinkConfiguration): Boolean {
        return shouldShowEarlyVerificationInFlowController
    }
}
