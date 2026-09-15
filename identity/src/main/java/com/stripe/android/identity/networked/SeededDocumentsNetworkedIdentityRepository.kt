package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import java.util.concurrent.TimeUnit

// #TODO - Networked Identity [NI-Contract]: remove once test-mode accounts with saved documents exist.

/**
 * Debug-only: returns sample saved documents, because test-mode Link accounts can't have any.
 * Every other call goes to [delegate].
 */
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
            Result.success(NetworkedIdentityAssociationToken("$SEEDED_TOKEN_PREFIX$documentId"))
        } else {
            delegate.createAssociationToken(credentials, documentId)
        }
    }

    /** The save endpoints aren't deployed yet; a sample token lets the playground show the save flow. */
    override suspend fun createSaveAssociationToken(
        credentials: NetworkedIdentityCredentials,
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken> = Result.success(NetworkedIdentityAssociationToken(SEEDED_SAVE_TOKEN))

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
        const val SEEDED_TOKEN_PREFIX = "seeded_token_"
        const val SEEDED_SAVE_TOKEN = "${SEEDED_TOKEN_PREFIX}save"
        private const val DAYS_UNTIL_EXPIRATION = 365L
    }
}

/**
 * Debug-only: sample attachments satisfy document capture locally. Other fields still use the test backend;
 * final submission is simulated because the sample files do not exist there. [actions] shares this state, so
 * Networked Identity actions and the Identity flow see the same session.
 */
internal class SeededDocumentsIdentityRepository(
    private val delegate: IdentityRepository,
) : IdentityRepository by delegate {
    private var allowsSeededDocuments = false
    private var hasSeededDocument = false
    private var latestData: VerificationPageData? = null

    override suspend fun retrieveVerificationPage(id: String, ephemeralKey: String): VerificationPage =
        delegate.retrieveVerificationPage(id, ephemeralKey).also { page ->
            allowsSeededDocuments = !page.livemode
            hasSeededDocument = false
            latestData = VerificationPageData(
                id = page.id,
                objectType = OBJECT_TYPE,
                requirements = VerificationPageDataRequirements(
                    errors = emptyList(),
                    missings = page.requirements.missing,
                ),
                status = page.status.toDataStatus(),
                submitted = page.submitted,
                closed = false,
            )
        }

    override suspend fun postVerificationPageData(
        id: String,
        ephemeralKey: String,
        collectedDataParam: CollectedDataParam,
        clearDataParam: ClearDataParam,
    ): VerificationPageData =
        remember(delegate.postVerificationPageData(id, ephemeralKey, collectedDataParam, clearDataParam))

    override suspend fun postVerificationPageSubmit(id: String, ephemeralKey: String): VerificationPageData {
        val latest = latestData
        if (!hasSeededDocument || latest == null) return remember(delegate.postVerificationPageSubmit(id, ephemeralKey))
        val data = applyingSeededDocument(latest)
        if (!data.isOpen || !data.requirements.missings.isNullOrEmpty()) return data
        return data.copy(status = VerificationPageData.Status.PROCESSING, submitted = true, closed = true)
            .also { latestData = it }
    }

    override suspend fun verifyTestVerificationSession(
        id: String,
        ephemeralKey: String,
        simulateDelay: Boolean,
    ): VerificationPageData = remember(delegate.verifyTestVerificationSession(id, ephemeralKey, simulateDelay))

    override suspend fun unverifyTestVerificationSession(
        id: String,
        ephemeralKey: String,
        simulateDelay: Boolean,
    ): VerificationPageData = remember(delegate.unverifyTestVerificationSession(id, ephemeralKey, simulateDelay))

    override suspend fun generatePhoneOtp(id: String, ephemeralKey: String): VerificationPageData =
        remember(delegate.generatePhoneOtp(id, ephemeralKey))

    override suspend fun cannotVerifyPhoneOtp(id: String, ephemeralKey: String): VerificationPageData =
        remember(delegate.cannotVerifyPhoneOtp(id, ephemeralKey))

    fun actions(delegate: NetworkedIdentityActions): NetworkedIdentityActions = object : NetworkedIdentityActions {
        override val verificationSessionId: String
            get() = delegate.verificationSessionId

        override suspend fun attachDocument(associationToken: String): Result<VerificationPageData> {
            if (!associationToken.startsWith(SeededDocumentsNetworkedIdentityRepository.SEEDED_TOKEN_PREFIX)) {
                hasSeededDocument = false
                return remember(delegate.attachDocument(associationToken))
            }
            val latest = latestData
            if (!allowsSeededDocuments || latest == null || !latest.isOpen) {
                return Result.failure(IllegalStateException("Sample documents can only be attached in test mode."))
            }
            hasSeededDocument = true
            return Result.success(applyingSeededDocument(latest))
        }

        /**
         * A sample save token records nothing on the backend: the session is returned unchanged, so the save
         * flow can be shown in any mode without claiming a verification result.
         */
        override suspend fun prepareDocumentSave(associationToken: String): Result<VerificationPageData> {
            if (associationToken != SeededDocumentsNetworkedIdentityRepository.SEEDED_SAVE_TOKEN) {
                return remember(delegate.prepareDocumentSave(associationToken))
            }
            return Result.success(
                latestData?.let(::applyingSeededDocument) ?: VerificationPageData(
                    id = verificationSessionId,
                    objectType = OBJECT_TYPE,
                    requirements = VerificationPageDataRequirements(errors = emptyList(), missings = emptyList()),
                    status = VerificationPageData.Status.PROCESSING,
                    submitted = true,
                    closed = true,
                )
            )
        }

        override suspend fun skip(): Result<VerificationPageData> {
            val latest = latestData
            if (!allowsSeededDocuments || latest == null) {
                return remember(delegate.skip())
            }
            hasSeededDocument = false
            return Result.success(latest)
        }
    }

    private fun remember(data: VerificationPageData): VerificationPageData {
        latestData = data
        return applyingSeededDocument(data)
    }

    private fun remember(result: Result<VerificationPageData>): Result<VerificationPageData> =
        result.map(::remember)

    private val VerificationPageData.isOpen: Boolean
        get() = requirements.errors.isEmpty() && status == VerificationPageData.Status.REQUIRESINPUT && !closed

    private fun applyingSeededDocument(data: VerificationPageData): VerificationPageData {
        if (!hasSeededDocument) return data
        return data.copy(
            requirements = data.requirements.copy(
                missings = data.requirements.missings?.filter {
                    it != Requirement.IDDOCUMENTFRONT && it != Requirement.IDDOCUMENTBACK
                }
            )
        )
    }

    private fun VerificationPage.Status.toDataStatus() = when (this) {
        VerificationPage.Status.CANCELLED -> VerificationPageData.Status.CANCELED
        VerificationPage.Status.PROCESSING -> VerificationPageData.Status.PROCESSING
        VerificationPage.Status.REQUIRESINPUT -> VerificationPageData.Status.REQUIRESINPUT
        VerificationPage.Status.VERIFIED -> VerificationPageData.Status.VERIFIED
    }

    private companion object {
        const val OBJECT_TYPE = "identity.verification_page_data"
    }
}
