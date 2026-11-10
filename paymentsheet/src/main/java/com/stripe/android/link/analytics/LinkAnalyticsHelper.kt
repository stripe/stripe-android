package com.stripe.android.link.analytics

import com.stripe.android.link.LinkActivityResult

internal interface LinkAnalyticsHelper {
    fun onLinkLaunched(checkoutSessionId: String?)

    fun onLinkResult(linkActivityResult: LinkActivityResult, checkoutSessionId: String?)

    fun onLinkPopupSkipped(checkoutSessionId: String?)
}
