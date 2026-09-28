package com.stripe.android.uicore.image

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeCdnImageOptimizer : ImageOptimizer {
    override fun optimize(url: String, width: Int): String {
        return "https://img.stripecdn.com/cdn-cgi/image/format=auto,width=$width,dpr=3/$url"
    }
}
