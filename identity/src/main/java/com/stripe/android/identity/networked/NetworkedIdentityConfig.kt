package com.stripe.android.identity.networked

import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.networkedIdentityRoute

/** What the Networked Identity flow needs from the VerificationPage, with the host's debug options applied. */
internal data class NetworkedIdentityConfig(
    val route: NetworkedIdentityRoute,
    val saveEnabled: Boolean,
    /** The backend found a saved document for the provided email. */
    val savedDocumentAvailable: Boolean,
    val merchantPublishableKey: String?,
    val merchantEmail: String?,
    val phoneNumberAutofillHint: String?,
    val seedSavedDocuments: Boolean,
) {
    internal companion object {
        fun from(
            page: VerificationPage,
            options: IdentityVerificationSheet.Configuration.NetworkedIdentityOptions,
        ): NetworkedIdentityConfig {
            val debugRoute = when (options.debugRoute) {
                "reuse" -> NetworkedIdentityRoute.Reuse
                "save" -> NetworkedIdentityRoute.Save
                "none" -> NetworkedIdentityRoute.OrdinaryIdentity
                else -> null
            }
            // The debug options win so the PoC can run before the backend returns these fields.
            return NetworkedIdentityConfig(
                route = debugRoute ?: page.networkedIdentityRoute,
                saveEnabled = debugRoute?.let {
                    it == NetworkedIdentityRoute.Reuse || it == NetworkedIdentityRoute.Save
                } ?: (page.networkedIdentity?.saveEnabled == true),
                // The debug reuse route stands for a page that found a saved document.
                savedDocumentAvailable = debugRoute?.let { it == NetworkedIdentityRoute.Reuse }
                    ?: (page.networkedIdentity?.reuseAvailable == true),
                merchantPublishableKey = options.debugMerchantPublishableKey ?: page.merchantPublishableKey,
                merchantEmail = options.debugProvidedEmail ?: page.networkedIdentity?.lookupEmail,
                phoneNumberAutofillHint = page.networkedIdentity?.phoneNumberAutofillHint,
                // #TODO - Networked Identity [NI-Contract]: test-mode Link accounts with saved documents.
                seedSavedDocuments = options.debugSeedSavedDocuments,
            )
        }
    }
}

/**
 * This page's Networked Identity setup, or null when the page doesn't offer Networked Identity or the host didn't
 * opt in with [options].
 */
internal fun VerificationPage.networkedIdentityConfig(
    options: IdentityVerificationSheet.Configuration.NetworkedIdentityOptions?,
): NetworkedIdentityConfig? {
    return NetworkedIdentityConfig.from(this, options ?: return null).takeIf {
        it.route != NetworkedIdentityRoute.OrdinaryIdentity && !it.merchantPublishableKey.isNullOrBlank()
    }
}
