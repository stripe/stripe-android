package com.stripe.android.link.gate

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.model.ElementsSession

/**
 * Provider interface for Link feature settings and behaviors, handling
 * the logic for determining when to show special Link UI elements or trigger automatic
 * behaviors to enhance the Link payment experience.
 */
internal interface LinkGate {
    val useNativeLink: Boolean
    val useAttestationEndpoints: Boolean
    val suppress2faModal: Boolean
    val useInlineOtpInWalletButtons: Boolean
    val showRuxInFlowController: Boolean

    interface Factory {
        fun create(elementsSession: ElementsSession): LinkGate
        fun create(configuration: LinkConfiguration): LinkGate
    }
}
