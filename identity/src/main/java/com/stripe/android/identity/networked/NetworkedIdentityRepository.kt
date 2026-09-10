package com.stripe.android.identity.networked

internal interface NetworkedIdentityRepository {
    suspend fun lookup(
        email: String,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentityLookup>

    suspend fun signUp(request: NetworkedIdentitySignUpRequest): Result<NetworkedIdentitySignUpResponse>

    suspend fun startVerification(
        credentials: NetworkedIdentityCredentials,
        locale: String,
        accountPhoneNumber: String?,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentitySessionResponse>

    suspend fun confirmVerification(
        credentials: NetworkedIdentityCredentials,
        code: String,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentitySessionResponse>

    suspend fun listDocuments(credentials: NetworkedIdentityCredentials): Result<List<NetworkedIdentityDocument>>

    suspend fun createAssociationToken(
        credentials: NetworkedIdentityCredentials,
        documentId: String,
    ): Result<NetworkedIdentityAssociationToken>

    suspend fun logout(
        credentials: NetworkedIdentityCredentials,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentitySessionResponse>

    suspend fun extendSession(credentials: NetworkedIdentityCredentials): Result<NetworkedIdentityExtendSessionResponse>
}
