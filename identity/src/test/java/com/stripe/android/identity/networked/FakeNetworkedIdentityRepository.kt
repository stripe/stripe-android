package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import kotlinx.coroutines.CompletableDeferred

internal class FakeNetworkedIdentityRepository : NetworkedIdentityRepository {
    val documentCalls = Turbine<DocumentCall>()
    val tokenCalls = Turbine<TokenCall>()
    val saveTokenCalls = Turbine<SaveTokenCall>()

    override suspend fun listDocuments(
        credentials: NetworkedIdentityCredentials
    ): Result<List<NetworkedIdentityDocument>> {
        val call = DocumentCall(credentials, CompletableDeferred())
        documentCalls.add(call)
        return call.response.await()
    }

    override suspend fun createAssociationToken(
        credentials: NetworkedIdentityCredentials,
        documentId: String,
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken> {
        val call = TokenCall(credentials, documentId, verificationSessionId, CompletableDeferred())
        tokenCalls.add(call)
        return call.response.await()
    }

    override suspend fun createSaveAssociationToken(
        credentials: NetworkedIdentityCredentials,
        verificationSessionId: String
    ): Result<NetworkedIdentityAssociationToken> {
        val call = SaveTokenCall(credentials, verificationSessionId, CompletableDeferred())
        saveTokenCalls.add(call)
        return call.response.await()
    }

    fun ensureAllEventsConsumed() {
        documentCalls.ensureAllEventsConsumed()
        tokenCalls.ensureAllEventsConsumed()
        saveTokenCalls.ensureAllEventsConsumed()
    }

    data class DocumentCall(
        val credentials: NetworkedIdentityCredentials,
        val response: CompletableDeferred<Result<List<NetworkedIdentityDocument>>>
    )
    data class SaveTokenCall(
        val credentials: NetworkedIdentityCredentials,
        val verificationSessionId: String,
        val response: CompletableDeferred<Result<NetworkedIdentityAssociationToken>>
    )
    data class TokenCall(
        val credentials: NetworkedIdentityCredentials,
        val documentId: String,
        val verificationSessionId: String,
        val response: CompletableDeferred<Result<NetworkedIdentityAssociationToken>>
    )
}

internal fun niDocument(id: String = "document_1") = NetworkedIdentityDocument(
    id = id,
    documentType = NetworkedIdentityDocumentType.PASSPORT,
    redactedDocumentNumber = "***1234",
    expirationDate = 200,
    liveCaptured = true
)
