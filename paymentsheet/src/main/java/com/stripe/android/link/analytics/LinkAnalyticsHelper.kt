package com.stripe.android.link.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkActivityResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface LinkAnalyticsHelper {
    fun onLinkLaunched()

    fun onLinkResult(linkActivityResult: LinkActivityResult)

    fun onLinkPopupSkipped()
}
