package com.stripe.android.identity.networked

/**
 * Link login and session for Networked Identity. Link owns lookup, sign-in, sign-up, the session
 * secrets and their refresh; Identity only reads the Link session key it needs for Networked
 * Identity calls.
 */
internal interface NetworkedIdentityLinkSession {
    /** Must succeed before any other call. */
    suspend fun configure(merchantPublishableKey: String, merchantDisplayName: String): Result<Unit>

    /**
     * Presents Link's own sign-in and sign-up screens, including code verification, until the session is
     * verified. [phoneNumber] only prefills sign-up. Returns null when the user dismisses Link.
     */
    suspend fun authenticateWithLinkUI(
        email: String?,
        phoneNumber: String?,
        mode: NetworkedIdentityMode,
    ): Result<NetworkedIdentityLinkAccount?>
}

internal data class NetworkedIdentityLinkAccount(
    val email: String,
    val isVerified: Boolean,
    /** Null until Link has a session key. */
    val credentials: NetworkedIdentityCredentials?,
) {
    override fun toString(): String = "NetworkedIdentityLinkAccount(isVerified=$isVerified)"
}
