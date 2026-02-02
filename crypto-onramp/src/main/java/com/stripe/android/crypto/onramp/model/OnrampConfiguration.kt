package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkAppearance

/**
 * Configuration options required to initialize the Onramp flow.
 *
 * @property merchantDisplayName The display name to use for the merchant.
 * @property publishableKey The publishable key from the API dashboard to enable requests.
 * @property appearance Appearance settings for the PaymentSheet UI.
 * @property cryptoCustomerId The unique customer ID for crypto onramp.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OnrampConfiguration {
    private var merchantDisplayName: String? = null
    private var publishableKey: String? = null
    private var appearance: LinkAppearance? = null
    private var cryptoCustomerId: String? = null

    fun merchantDisplayName(merchantDisplayName: String) = apply {
        this.merchantDisplayName = merchantDisplayName
    }

    fun publishableKey(publishableKey: String) = apply {
        this.publishableKey = publishableKey
    }

    fun appearance(appearance: LinkAppearance) = apply {
        this.appearance = appearance
    }

    fun cryptoCustomerId(cryptoCustomerId: String?) = apply {
        this.cryptoCustomerId = cryptoCustomerId
    }

    internal class State(
        val merchantDisplayName: String,
        val publishableKey: String,
        val appearance: LinkAppearance,
        val cryptoCustomerId: String? = null
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
            cryptoCustomerId = cryptoCustomerId,
        )
    }
}
