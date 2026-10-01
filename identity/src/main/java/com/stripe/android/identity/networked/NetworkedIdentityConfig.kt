package com.stripe.android.identity.networked

import android.os.Parcelable
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.networkedIdentityRoute
import kotlinx.parcelize.Parcelize

/** What the Networked Identity flow needs from the VerificationPage, with the host's debug overrides applied. */
internal data class NetworkedIdentityConfig(
    val route: NetworkedIdentityRoute,
    val saveAvailable: Boolean,
    val merchantPublishableKey: String?,
    val merchantEmail: String?,
    val seedSavedDocuments: Boolean,
    /** Sign in and sign up with Link's own screens; Identity's sheet only shows the steps after that. */
    val usesLinkUI: Boolean,
) {
    internal companion object {
        fun from(page: VerificationPage, overrides: NetworkedIdentityDebugOverrides?): NetworkedIdentityConfig =
            NetworkedIdentityConfig(
                route = overrides?.route ?: page.networkedIdentityRoute,
                saveAvailable = overrides?.route?.let {
                    it == NetworkedIdentityRoute.Reuse || it == NetworkedIdentityRoute.Save
                } ?: (page.networkedIdentity?.saveAvailable == true),
                // The debug overrides win so the PoC can run before the backend returns these fields.
                merchantPublishableKey = overrides?.merchantPublishableKey ?: page.merchantPublishableKey,
                merchantEmail = overrides?.merchantEmail ?: page.networkedIdentity?.email
                    ?: page.providedDetails?.email,
                // #TODO - Networked Identity [NI-Contract]: test-mode Link accounts with saved documents.
                seedSavedDocuments = overrides?.seedSavedDocuments == true,
                usesLinkUI = overrides?.usesLinkUI == true,
            )
    }
}

/** Debug-only values for the PoC, supplied by the host app until the backend provides them. */
@Parcelize
internal data class NetworkedIdentityDebugOverrides(
    val route: NetworkedIdentityRoute?,
    val merchantPublishableKey: String?,
    val merchantEmail: String?,
    val seedSavedDocuments: Boolean,
    val usesLinkUI: Boolean,
) : Parcelable

internal fun IdentityVerificationSheet.Configuration.NetworkedIdentityOptions.toDebugOverrides() =
    NetworkedIdentityDebugOverrides(
        route = when (debugRoute) {
            "reuse" -> NetworkedIdentityRoute.Reuse
            "save" -> NetworkedIdentityRoute.Save
            "none" -> NetworkedIdentityRoute.OrdinaryIdentity
            else -> null
        },
        merchantPublishableKey = debugMerchantPublishableKey,
        merchantEmail = debugProvidedEmail,
        seedSavedDocuments = debugSeedSavedDocuments,
        usesLinkUI = debugUseLinkUI,
    )
