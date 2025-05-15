package com.stripe.android.link

import com.stripe.android.link.domain.LinkProminenceFeatureProvider
import com.stripe.android.paymentsheet.state.LinkState

internal class FakeLinkProminenceFeatureProvider : LinkProminenceFeatureProvider {

    override fun show2FADialogOnLinkSelectedInFlowController(linkState: LinkState): Boolean {
        return false
    }
}
