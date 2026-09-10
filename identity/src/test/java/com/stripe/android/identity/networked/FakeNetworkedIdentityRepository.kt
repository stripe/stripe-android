package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import kotlinx.coroutines.CompletableDeferred

internal class FakeNetworkedIdentityRepository : NetworkedIdentityRepository {
    val lookupCalls = Turbine<LookupCall>()
    val startCalls = Turbine<StartCall>()
    val confirmCalls = Turbine<ConfirmCall>()
    val documentCalls = Turbine<DocumentCall>()
    val logoutCalls = Turbine<LogoutCall>()
    val unsupportedCalls = Turbine<String>()

    override suspend fun lookup(email: String, authSessionSecrets: List<String>): Result<NetworkedIdentityLookup> {
        val call = LookupCall(email, authSessionSecrets, CompletableDeferred())
        lookupCalls.add(call)
        return call.response.await()
    }

    override suspend fun startVerification(
        credentials: NetworkedIdentityCredentials,
        locale: String,
        accountPhoneNumber: String?,
        authSessionSecrets: List<String>
    ): Result<NetworkedIdentitySessionResponse> {
        val call = StartCall(credentials, locale, accountPhoneNumber, authSessionSecrets, CompletableDeferred())
        startCalls.add(call)
        return call.response.await()
    }

    override suspend fun confirmVerification(
        credentials: NetworkedIdentityCredentials,
        code: String,
        authSessionSecrets: List<String>
    ): Result<NetworkedIdentitySessionResponse> {
        val call = ConfirmCall(credentials, code, authSessionSecrets, CompletableDeferred())
        confirmCalls.add(call)
        return call.response.await()
    }

    override suspend fun listDocuments(
        credentials: NetworkedIdentityCredentials
    ): Result<List<NetworkedIdentityDocument>> {
        val call = DocumentCall(credentials, CompletableDeferred())
        documentCalls.add(call)
        return call.response.await()
    }

    override suspend fun logout(
        credentials: NetworkedIdentityCredentials,
        authSessionSecrets: List<String>
    ): Result<NetworkedIdentitySessionResponse> {
        logoutCalls.add(LogoutCall(credentials, authSessionSecrets))
        return Result.success(niResponse())
    }

    override suspend fun signUp(request: NetworkedIdentitySignUpRequest): Result<NetworkedIdentitySignUpResponse> {
        unsupportedCalls.add("signUp")
        return Result.failure(IllegalStateException("Unexpected signup"))
    }

    override suspend fun createAssociationToken(
        credentials: NetworkedIdentityCredentials,
        documentId: String
    ): Result<NetworkedIdentityAssociationToken> {
        unsupportedCalls.add("createAssociationToken")
        return Result.failure(IllegalStateException("Unexpected association token"))
    }

    override suspend fun extendSession(
        credentials: NetworkedIdentityCredentials
    ): Result<NetworkedIdentityExtendSessionResponse> {
        unsupportedCalls.add("extendSession")
        return Result.failure(IllegalStateException("Unexpected session extension"))
    }

    fun ensureAllEventsConsumed() {
        lookupCalls.ensureAllEventsConsumed()
        startCalls.ensureAllEventsConsumed()
        confirmCalls.ensureAllEventsConsumed()
        documentCalls.ensureAllEventsConsumed()
        logoutCalls.ensureAllEventsConsumed()
        unsupportedCalls.ensureAllEventsConsumed()
    }

    data class LookupCall(
        val email: String,
        val authSessionSecrets: List<String>,
        val response: CompletableDeferred<Result<NetworkedIdentityLookup>>
    )
    data class StartCall(
        val credentials: NetworkedIdentityCredentials,
        val locale: String,
        val accountPhoneNumber: String?,
        val authSessionSecrets: List<String>,
        val response: CompletableDeferred<Result<NetworkedIdentitySessionResponse>>
    )
    data class ConfirmCall(
        val credentials: NetworkedIdentityCredentials,
        val code: String,
        val authSessionSecrets: List<String>,
        val response: CompletableDeferred<Result<NetworkedIdentitySessionResponse>>
    )
    data class DocumentCall(
        val credentials: NetworkedIdentityCredentials,
        val response: CompletableDeferred<Result<List<NetworkedIdentityDocument>>>
    )
    data class LogoutCall(val credentials: NetworkedIdentityCredentials, val authSessionSecrets: List<String>)
}

internal fun niSession(
    clientSecret: String = "session_lookup",
    verificationSessions: List<NetworkedIdentityVerificationSession> = emptyList()
) = NetworkedIdentityConsumerSession(
    clientSecret = clientSecret,
    emailAddress = "person@example.com",
    redactedPhoneNumber = "***1234",
    redactedFormattedPhoneNumber = "(***) ***-1234",
    unredactedPhoneNumber = "+15555551234",
    phoneNumberCountry = "US",
    verificationSessions = verificationSessions
)

internal fun niFound(
    session: NetworkedIdentityConsumerSession = niSession(),
    authSessionClientSecret: String? = "auth_lookup"
) = NetworkedIdentityLookup.Found(
    session = session,
    publishableKey = "pk_consumer",
    accountId = "account_123",
    authSessionClientSecret = authSessionClientSecret,
    emailOtpRequiresAdditionalInfo = null,
    emailOtpVerifyPhoneDespiteSmsOtp = null,
    experiments = emptyList()
)

internal fun niResponse(
    clientSecret: String = "session_started",
    verificationSessions: List<NetworkedIdentityVerificationSession> = listOf(niSms()),
    authSessionClientSecret: String? = "auth_started"
) = NetworkedIdentitySessionResponse(niSession(clientSecret, verificationSessions), authSessionClientSecret)

internal fun niSms(
    id: String? = "sms_fresh",
    state: NetworkedIdentityVerificationState = NetworkedIdentityVerificationState.STARTED,
    type: NetworkedIdentityVerificationType = NetworkedIdentityVerificationType.SMS
) = NetworkedIdentityVerificationSession(id, type, state, null)

internal fun niDocument(id: String = "document_1") = NetworkedIdentityDocument(
    id = id,
    documentType = NetworkedIdentityDocumentType.PASSPORT,
    created = 10,
    country = "US",
    region = null,
    redactedDocumentNumber = "***1234",
    expirationDate = 200,
    liveCaptured = true
)
