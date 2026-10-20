package com.stripe.android.identity.networked

import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute

/**
 * What the Identity screens offer, driven by the VerificationPage: the route decides whether reuse and saving are
 * offered, and a saved document the backend found adds the "Saved ID" chip.
 */
internal data class NetworkedIdentityEntry(
    val offersReuse: Boolean,
    val offersSave: Boolean,
    /** The "Saved ID" chip's email: the handed-in session's, or the provided one. Null without a found document. */
    val savedIdEmail: String?,
) {
    internal companion object {
        operator fun invoke(
            config: NetworkedIdentityConfig,
            handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff?,
            route: NetworkedIdentityRoute,
        ): NetworkedIdentityEntry {
            val hasKey = !config.merchantPublishableKey.isNullOrBlank()
            val offersReuse = hasKey && route == NetworkedIdentityRoute.Reuse
            val offersSaveRoute = route == NetworkedIdentityRoute.Reuse || route == NetworkedIdentityRoute.Save
            return NetworkedIdentityEntry(
                offersReuse = offersReuse,
                offersSave = hasKey && config.saveEnabled && offersSaveRoute,
                savedIdEmail = (handoff?.email ?: config.merchantEmail)
                    ?.takeIf { offersReuse && config.savedDocumentAvailable && it.isNotBlank() },
            )
        }
    }
}
