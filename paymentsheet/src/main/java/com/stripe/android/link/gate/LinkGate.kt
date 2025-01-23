package com.stripe.android.link.gate

import com.stripe.android.link.LinkConfiguration

internal interface LinkGate {
    val useNativeLink: Boolean
    val useAttestationEndpoints: Boolean

    fun interface Factory {
        fun create(configuration: LinkConfiguration): LinkGate
    }
}
