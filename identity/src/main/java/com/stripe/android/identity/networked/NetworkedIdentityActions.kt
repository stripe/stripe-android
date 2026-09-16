package com.stripe.android.identity.networked

import com.stripe.android.identity.networking.models.VerificationPageData

/** Identity actions use the verification session's ephemeral key, never Link session credentials. */
internal interface NetworkedIdentityActions {
    val verificationSessionId: String

    suspend fun attachDocument(associationToken: String): Result<VerificationPageData>

    suspend fun prepareDocumentSave(associationToken: String): Result<VerificationPageData>

    suspend fun skip(): Result<VerificationPageData>
}
