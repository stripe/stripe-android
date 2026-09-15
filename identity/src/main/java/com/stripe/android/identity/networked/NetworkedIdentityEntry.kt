package com.stripe.android.identity.networked

import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute

/**
 * What the Identity screens offer, driven by data: a handed-in session or a lookup of the provided email
 * decides whether the intro offers reuse, and the route decides whether saving is still offered.
 */
internal data class NetworkedIdentityEntry(
    val reuseAvailable: Boolean,
    val offersSave: Boolean,
    /** The Link account's email: the handed-in session's, or the provided email once a lookup found it. */
    val accountEmail: String?,
    /** No handed-in session and no provided email: offer Link without a chip and ask for the email. */
    val needsEmail: Boolean,
) {
    val linkAvailable: Boolean
        get() = reuseAvailable || offersSave

    /** The intro offers reuse when there's an account to reuse, or no email to check. */
    val offersReuse: Boolean
        get() = reuseAvailable && (accountEmail != null || needsEmail)

    internal companion object {
        operator fun invoke(
            config: NetworkedIdentityConfig,
            handoff: IdentityVerificationSheet.Configuration.LinkSessionHandoff?,
            route: NetworkedIdentityRoute,
            accountEmail: String?,
        ): NetworkedIdentityEntry {
            val hasKey = !config.merchantPublishableKey.isNullOrBlank()
            val offersSaveRoute = route == NetworkedIdentityRoute.Reuse || route == NetworkedIdentityRoute.Save
            return NetworkedIdentityEntry(
                reuseAvailable = hasKey && route == NetworkedIdentityRoute.Reuse,
                offersSave = hasKey &&
                    (route == NetworkedIdentityRoute.ResumeSave || (config.saveAvailable && offersSaveRoute)),
                accountEmail = accountEmail ?: handoff?.email,
                needsEmail = handoff == null && config.merchantEmail.isNullOrBlank(),
            )
        }
    }
}
