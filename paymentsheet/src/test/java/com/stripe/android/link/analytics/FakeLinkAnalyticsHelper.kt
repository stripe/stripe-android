package com.stripe.android.link.analytics

import com.stripe.android.link.LinkActivityResult

internal open class FakeLinkAnalyticsHelper : LinkAnalyticsHelper {
    override fun onLinkLaunched(checkoutSessionId: String?) {
        TODO("Not yet implemented")
    }

    override fun onLinkResult(linkActivityResult: LinkActivityResult, checkoutSessionId: String?) {
        TODO("Not yet implemented")
    }

    override fun onLinkPopupSkipped(checkoutSessionId: String?) {
        TODO("Not yet implemented")
    }
}
