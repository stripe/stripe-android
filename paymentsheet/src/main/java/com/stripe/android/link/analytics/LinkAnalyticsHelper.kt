package com.stripe.android.link.analytics

import com.stripe.android.link.LinkActivityResult

internal interface LinkAnalyticsHelper {
    fun onLinkLaunched()

    fun onLinkResult(linkActivityResult: LinkActivityResult)

    fun onLinkPopupSkipped()
}
