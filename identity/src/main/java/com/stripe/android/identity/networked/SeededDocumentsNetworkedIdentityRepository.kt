package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import java.util.concurrent.TimeUnit

/**
 * Debug-only: returns sample saved documents, because test-mode Link accounts can't have any.
 * Every other call goes to [delegate].
 */
// #TODO - Networked Identity [NI-Contract]: remove once test-mode accounts with saved documents exist.
internal class SeededDocumentsNetworkedIdentityRepository(
    private val delegate: NetworkedIdentityRepository,
    private val currentTimeSeconds: () -> Long,
) : NetworkedIdentityRepository by delegate {

    override suspend fun listDocuments(
        credentials: NetworkedIdentityCredentials,
    ): Result<List<NetworkedIdentityDocument>> {
        val now = currentTimeSeconds()
        val expiration = now + TimeUnit.DAYS.toSeconds(DAYS_UNTIL_EXPIRATION)
        return Result.success(
            listOf(
                seededDocument("passport", NetworkedIdentityDocumentType.PASSPORT, now, expiration),
                seededDocument("driving_license", NetworkedIdentityDocumentType.DRIVING_LICENSE, now, expiration),
            )
        )
    }

    override suspend fun createAssociationToken(
        credentials: NetworkedIdentityCredentials,
        documentId: String,
    ): Result<NetworkedIdentityAssociationToken> {
        return if (documentId.startsWith(SEEDED_ID_PREFIX)) {
            Result.success(NetworkedIdentityAssociationToken("seeded_token_$documentId"))
        } else {
            delegate.createAssociationToken(credentials, documentId)
        }
    }

    private fun seededDocument(
        name: String,
        type: NetworkedIdentityDocumentType,
        created: Long,
        expiration: Long,
    ) = NetworkedIdentityDocument(
        id = "$SEEDED_ID_PREFIX$name",
        documentType = type,
        created = created,
        country = "US",
        region = null,
        redactedDocumentNumber = "••••1234",
        expirationDate = expiration,
        liveCaptured = true,
    )

    internal companion object {
        const val SEEDED_ID_PREFIX = "seeded_iddoc_"
        private const val DAYS_UNTIL_EXPIRATION = 365L
    }
}

/**
 * Debug-only: seeded documents have no real association token, so attaching one succeeds without a
 * request and reports no requirements; the next verification response still lists the document.
 * Every other call goes to [delegate].
 */
// #TODO - Networked Identity [NI-Contract]: remove together with SeededDocumentsNetworkedIdentityRepository.
internal class SeededDocumentsNetworkedIdentityActions(
    private val delegate: NetworkedIdentityActions,
) : NetworkedIdentityActions by delegate {

    override suspend fun attachDocument(associationToken: String): Result<VerificationPageData> {
        if (!associationToken.startsWith(SEEDED_TOKEN_PREFIX)) return delegate.attachDocument(associationToken)
        return Result.success(
            VerificationPageData(
                id = verificationSessionId,
                objectType = "identity.verification_page_data",
                requirements = VerificationPageDataRequirements(errors = emptyList(), missings = emptyList()),
                status = VerificationPageData.Status.REQUIRESINPUT,
                submitted = false,
                closed = false,
            )
        )
    }

    private companion object {
        const val SEEDED_TOKEN_PREFIX = "seeded_token_"
    }
}
