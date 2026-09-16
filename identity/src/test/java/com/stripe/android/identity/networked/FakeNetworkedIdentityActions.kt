package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import kotlinx.coroutines.CompletableDeferred

internal class FakeNetworkedIdentityActions : NetworkedIdentityActions {
    override val verificationSessionId: String = "vs_target"
    val attachCalls = Turbine<TokenCall>()
    val prepareSaveCalls = Turbine<TokenCall>()
    val skipCalls = Turbine<SkipCall>()

    override suspend fun attachDocument(associationToken: String): Result<VerificationPageData> {
        val call = TokenCall(associationToken, CompletableDeferred())
        attachCalls.add(call)
        return call.response.await()
    }

    override suspend fun prepareDocumentSave(associationToken: String): Result<VerificationPageData> {
        val call = TokenCall(associationToken, CompletableDeferred())
        prepareSaveCalls.add(call)
        return call.response.await()
    }

    override suspend fun skip(): Result<VerificationPageData> {
        val call = SkipCall(CompletableDeferred())
        skipCalls.add(call)
        return call.response.await()
    }

    fun ensureAllEventsConsumed() {
        attachCalls.ensureAllEventsConsumed()
        prepareSaveCalls.ensureAllEventsConsumed()
        skipCalls.ensureAllEventsConsumed()
    }

    data class TokenCall(
        val associationToken: String,
        val response: CompletableDeferred<Result<VerificationPageData>>
    )

    data class SkipCall(val response: CompletableDeferred<Result<VerificationPageData>>)
}

internal fun niActionPageData(id: String = "vs_target") = VerificationPageData(
    id = id,
    objectType = "identity.verification_page_data",
    requirements = VerificationPageDataRequirements(errors = emptyList(), missings = listOf(Requirement.FACE)),
    status = VerificationPageData.Status.REQUIRESINPUT,
    submitted = false,
    closed = false
)
