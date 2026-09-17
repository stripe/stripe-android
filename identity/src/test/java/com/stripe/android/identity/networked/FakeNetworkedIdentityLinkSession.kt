package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import kotlinx.coroutines.CompletableDeferred

internal class FakeNetworkedIdentityLinkSession : NetworkedIdentityLinkSession {
    val configureCalls = Turbine<ConfigureCall>()
    val restoreCalls = Turbine<RestoreCall>()
    val lookupCalls = Turbine<LookupCall>()
    val startVerificationCalls = Turbine<StartVerificationCall>()
    val confirmVerificationCalls = Turbine<ConfirmVerificationCall>()
    val signUpCalls = Turbine<SignUpCall>()
    val logOutCalls = Turbine<Unit>()

    override suspend fun configure(merchantPublishableKey: String, merchantDisplayName: String): Result<Unit> {
        val call = ConfigureCall(merchantPublishableKey, merchantDisplayName, CompletableDeferred())
        configureCalls.add(call)
        return call.response.await()
    }

    override suspend fun restore(credentials: NetworkedIdentityCredentials): Result<NetworkedIdentityLinkAccount> {
        val call = RestoreCall(credentials, CompletableDeferred())
        restoreCalls.add(call)
        return call.response.await()
    }

    override suspend fun lookup(email: String): Result<NetworkedIdentityLinkAccount?> {
        val call = LookupCall(email, CompletableDeferred())
        lookupCalls.add(call)
        return call.response.await()
    }

    override suspend fun startVerification(isResend: Boolean): Result<NetworkedIdentityLinkAccount> {
        val call = StartVerificationCall(isResend, CompletableDeferred())
        startVerificationCalls.add(call)
        return call.response.await()
    }

    override suspend fun confirmVerification(code: String): Result<NetworkedIdentityLinkAccount> {
        val call = ConfirmVerificationCall(code, CompletableDeferred())
        confirmVerificationCalls.add(call)
        return call.response.await()
    }

    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
    ): Result<NetworkedIdentityLinkAccount> {
        val call = SignUpCall(email, phoneNumber, country, name, CompletableDeferred())
        signUpCalls.add(call)
        return call.response.await()
    }

    override suspend fun logOut(): Result<Unit> {
        logOutCalls.add(Unit)
        return Result.success(Unit)
    }

    fun ensureAllEventsConsumed() {
        configureCalls.ensureAllEventsConsumed()
        restoreCalls.ensureAllEventsConsumed()
        lookupCalls.ensureAllEventsConsumed()
        startVerificationCalls.ensureAllEventsConsumed()
        confirmVerificationCalls.ensureAllEventsConsumed()
        signUpCalls.ensureAllEventsConsumed()
        logOutCalls.ensureAllEventsConsumed()
    }

    data class ConfigureCall(
        val merchantPublishableKey: String,
        val merchantDisplayName: String,
        val response: CompletableDeferred<Result<Unit>>,
    )

    data class RestoreCall(
        val credentials: NetworkedIdentityCredentials,
        val response: CompletableDeferred<Result<NetworkedIdentityLinkAccount>>,
    )

    data class LookupCall(
        val email: String,
        val response: CompletableDeferred<Result<NetworkedIdentityLinkAccount?>>,
    )

    data class StartVerificationCall(
        val isResend: Boolean,
        val response: CompletableDeferred<Result<NetworkedIdentityLinkAccount>>,
    )

    data class ConfirmVerificationCall(
        val code: String,
        val response: CompletableDeferred<Result<NetworkedIdentityLinkAccount>>,
    )

    data class SignUpCall(
        val email: String,
        val phoneNumber: String,
        val country: String,
        val name: String?,
        val response: CompletableDeferred<Result<NetworkedIdentityLinkAccount>>,
    )
}

internal fun niAccount(
    isVerified: Boolean = false,
    credentials: NetworkedIdentityCredentials? = NetworkedIdentityCredentials(
        publishableKey = "pk_consumer",
        sessionClientSecret = "session_secret",
    ),
) = NetworkedIdentityLinkAccount(
    email = "person@example.com",
    redactedPhoneNumber = "(***) ***-1234",
    isVerified = isVerified,
    credentials = credentials,
)
