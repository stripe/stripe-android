package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import com.stripe.android.identity.networking.models.networkedIdentityRoute

/** Identity actions use the verification session's ephemeral key, never Link session credentials. */
internal interface NetworkedIdentityActions {
    val verificationSessionId: String

    suspend fun attachDocument(associationToken: String): Result<VerificationPageData>

    suspend fun prepareDocumentSave(associationToken: String): Result<VerificationPageData>

    suspend fun skip(): Result<VerificationPageData>

    /**
     * Reads the session back after a redemption whose outcome is unknown: the verification if the write that leads
     * to [route] took effect, null otherwise.
     */
    suspend fun verificationIfCommitted(route: NetworkedIdentityRoute): Result<VerificationPageData?>
}

/**
 * This page as a verification, if the Networked Identity write that leads to [route] took effect: a reuse once the
 * shared document satisfies the document requirement, a save once its consent is recorded.
 */
internal fun VerificationPage.verificationIfCommitted(route: NetworkedIdentityRoute): VerificationPageData? {
    val missing = requirements.missing
    val documentMissing = Requirement.IDDOCUMENTFRONT in missing || Requirement.IDDOCUMENTBACK in missing
    if (networkedIdentityRoute != route || (route == NetworkedIdentityRoute.ResumeReuse && documentMissing)) {
        return null
    }
    return toVerificationPageData()
}

/** This page as the [VerificationPageData] a VerificationPage action returns. */
internal fun VerificationPage.toVerificationPageData() = VerificationPageData(
    id = id,
    objectType = "identity.verification_page_data",
    requirements = VerificationPageDataRequirements(errors = emptyList(), missings = requirements.missing),
    status = when (status) {
        VerificationPage.Status.CANCELLED -> VerificationPageData.Status.CANCELED
        VerificationPage.Status.PROCESSING -> VerificationPageData.Status.PROCESSING
        VerificationPage.Status.REQUIRESINPUT -> VerificationPageData.Status.REQUIRESINPUT
        VerificationPage.Status.VERIFIED -> VerificationPageData.Status.VERIFIED
    },
    submitted = submitted,
    // A submitted verification takes no more writes.
    closed = submitted,
)
