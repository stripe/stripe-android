package com.stripe.android.identity.networked

internal interface NetworkedIdentityRepository {
    suspend fun listDocuments(credentials: NetworkedIdentityCredentials): Result<List<NetworkedIdentityDocument>>

    suspend fun createAssociationToken(
        credentials: NetworkedIdentityCredentials,
        documentId: String,
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken>

    suspend fun createSaveAssociationToken(
        credentials: NetworkedIdentityCredentials,
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken>
}
