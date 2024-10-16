package com.stripe.android.link

object NativeLinkEnabled {
    var enabled: Boolean = false
    operator fun invoke() = enabled
}
