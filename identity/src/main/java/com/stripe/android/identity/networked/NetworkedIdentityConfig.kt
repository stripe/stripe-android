package com.stripe.android.identity.networked

import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.networkedIdentityRoute

/** What the Networked Identity flow needs from the VerificationPage and the host's options. */
internal data class NetworkedIdentityConfig(
    val route: NetworkedIdentityRoute,
    val saveEnabled: Boolean,
    /** The backend found a saved document for the provided email. */
    val savedDocumentAvailable: Boolean,
    val merchantPublishableKey: String?,
    val merchantEmail: String?,
    val phoneNumberAutofillHint: String?,
) {
    internal companion object {
        fun from(
            page: VerificationPage,
            options: IdentityVerificationSheet.Configuration.NetworkedIdentityOptions,
        ): NetworkedIdentityConfig = NetworkedIdentityConfig(
            route = page.networkedIdentityRoute,
            saveEnabled = page.networkedIdentity?.saveEnabled == true,
            savedDocumentAvailable = page.networkedIdentity?.reuseAvailable == true,
            merchantPublishableKey = options.merchantPublishableKey,
            merchantEmail = page.networkedIdentity?.lookupEmail,
            phoneNumberAutofillHint = page.networkedIdentity?.phoneNumberAutofillHint,
        )
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
