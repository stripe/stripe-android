package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import java.util.concurrent.TimeUnit

// #TODO - Networked Identity [NI-Contract]: remove once test-mode accounts with saved documents exist.

/**
 * Debug-only: sample saved documents and tokens, because test-mode Link accounts can't have saved documents and
 * the save endpoints aren't deployed yet. [SeededDocumentsIdentityRepository] redeems the tokens locally.
 */
internal class SeededDocumentsNetworkedIdentityRepository(
    private val currentTimeSeconds: () -> Long,
) : NetworkedIdentityRepository {

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
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken> =
        Result.success(NetworkedIdentityAssociationToken("sample_$documentId"))

    override suspend fun createSaveAssociationToken(
        credentials: NetworkedIdentityCredentials,
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken> = Result.success(NetworkedIdentityAssociationToken("sample_save"))

    private fun seededDocument(
        name: String,
        type: NetworkedIdentityDocumentType,
        created: Long,
        expiration: Long,
    ) = NetworkedIdentityDocument(
        id = "sample_$name",
        documentType = type,
        created = created,
        country = "US",
        region = null,
        redactedDocumentNumber = "••••1234",
        expirationDate = expiration,
        liveCaptured = true,
    )

    private companion object {
        const val DAYS_UNTIL_EXPIRATION = 365L
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
            latestData = page.toVerificationPageData()
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
            val latest = latestData
            if (!allowsSeededDocuments || latest == null || !latest.isOpen) {
                return Result.failure(IllegalStateException("Sample documents can only be attached in test mode."))
            }
            hasSeededDocument = true
            return Result.success(applyingSeededDocument(latest))
        }

        /** Records nothing: the session is returned unchanged, without claiming a verification result. */
        override suspend fun prepareDocumentSave(associationToken: String): Result<VerificationPageData> {
            val latest = latestData ?: return Result.failure(IllegalStateException("The verification isn't loaded."))
            return Result.success(applyingSeededDocument(latest))
        }

        override suspend fun skip(): Result<VerificationPageData> {
            val latest = latestData
            if (!allowsSeededDocuments || latest == null) {
                return remember(delegate.skip())
            }
            hasSeededDocument = false
            return Result.success(latest)
        }

        override suspend fun verificationIfCommitted(route: NetworkedIdentityRoute): Result<VerificationPageData?> =
            delegate.verificationIfCommitted(route)
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
}
