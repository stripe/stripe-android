package com.stripe.android.identity.networked

/**
 * Link login and session for Networked Identity. Link owns lookup, one-time codes, sign-up, the
 * session secrets and their refresh; Identity only reads the consumer credentials it needs for
 * Networked Identity calls.
 */
internal interface NetworkedIdentityLinkSession {
    /** Must succeed before any other call. */
    suspend fun configure(merchantPublishableKey: String, merchantDisplayName: String): Result<Unit>

    /** Restores a session handed in from outside Identity, e.g. by crypto onramp. */
    suspend fun restore(credentials: NetworkedIdentityCredentials): Result<NetworkedIdentityLinkAccount>

    /** Returns null when the email has no Link account. */
    suspend fun lookup(email: String): Result<NetworkedIdentityLinkAccount?>

    suspend fun startVerification(isResend: Boolean): Result<NetworkedIdentityLinkAccount>

    suspend fun confirmVerification(code: String): Result<NetworkedIdentityLinkAccount>

    suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
    ): Result<NetworkedIdentityLinkAccount>

    suspend fun logOut(): Result<Unit>
}

internal data class NetworkedIdentityLinkAccount(
    val email: String,
    val redactedPhoneNumber: String,
    val isVerified: Boolean,
    /** Null until Link has both the session secret and the consumer publishable key. */
    val credentials: NetworkedIdentityCredentials?,
) {
    override fun toString(): String = "NetworkedIdentityLinkAccount(isVerified=$isVerified)"
}
