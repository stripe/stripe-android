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
    val merchantPublishableKey: String,
    val merchantEmail: String?,
    val phoneNumberAutofillHint: String?,
)

/**
 * This page's Networked Identity setup, or null when the page doesn't offer Networked Identity or the host didn't
 * opt in with [options].
 */
internal fun VerificationPage.networkedIdentityConfig(
    options: IdentityVerificationSheet.Configuration.NetworkedIdentityOptions?,
): NetworkedIdentityConfig? {
    val merchantPublishableKey = options?.merchantPublishableKey?.takeIf { it.isNotBlank() } ?: return null
    val route = networkedIdentityRoute.takeIf { it != NetworkedIdentityRoute.OrdinaryIdentity } ?: return null
    return NetworkedIdentityConfig(
        route = route,
        saveEnabled = networkedIdentity?.saveEnabled == true,
        savedDocumentAvailable = networkedIdentity?.reuseAvailable == true,
        merchantPublishableKey = merchantPublishableKey,
        merchantEmail = networkedIdentity?.lookupEmail,
        phoneNumberAutofillHint = networkedIdentity?.phoneNumberAutofillHint,
    )
}
