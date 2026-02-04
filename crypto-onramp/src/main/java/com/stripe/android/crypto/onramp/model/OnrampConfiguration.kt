package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkAppearance

/**
 * Configuration options required to initialize the Onramp flow.
 *
 * @property merchantDisplayName The display name to use for the merchant.
 * @property publishableKey The publishable key from the API dashboard to enable requests.
 * @property appearance Appearance settings for the PaymentSheet UI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampConfiguration {
    private var merchantDisplayName: String? = null
    private var publishableKey: String? = null
    private var appearance: LinkAppearance? = null

    /**
     * Sets the display name of the merchant.
     */
    fun merchantDisplayName(merchantDisplayName: String) = apply {
        this.merchantDisplayName = merchantDisplayName
    }

    /**
     * Sets the publishable key of the merchant.
     */
    fun publishableKey(publishableKey: String) = apply {
        this.publishableKey = publishableKey
    }

    /**
     * Sets appearance settings for the payment sheet user interface presented by Stripe.
     * This does have a default appearance.
     */
    fun appearance(appearance: LinkAppearance) = apply {
        this.appearance = appearance
    }

    internal class State(
        val merchantDisplayName: String,
        val publishableKey: String,
        val appearance: LinkAppearance,
    )

    internal fun build(): State {
        return State(
            merchantDisplayName = requireNotNull(merchantDisplayName) {
                "merchantDisplayName must not be null"
            },
            publishableKey = requireNotNull(publishableKey) {
                "publishableKey must not be null"
            },
            appearance = appearance ?: LinkAppearance(),
        )
    }
}
