package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import kotlinx.coroutines.CompletableDeferred

internal class FakeNetworkedIdentityLinkSession : NetworkedIdentityLinkSession {
    val configureCalls = Turbine<ConfigureCall>()
    val restoreCalls = Turbine<RestoreCall>()
    val lookupCalls = Turbine<LookupCall>()
    val linkUICalls = Turbine<LinkUICall>()

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

    override suspend fun authenticateWithLinkUI(
        email: String?,
        phoneNumber: String?,
        mode: NetworkedIdentityMode,
    ): Result<NetworkedIdentityLinkAccount?> {
        val call = LinkUICall(email, phoneNumber, mode, CompletableDeferred())
        linkUICalls.add(call)
        return call.response.await()
    }

    fun ensureAllEventsConsumed() {
        configureCalls.ensureAllEventsConsumed()
        restoreCalls.ensureAllEventsConsumed()
        lookupCalls.ensureAllEventsConsumed()
        linkUICalls.ensureAllEventsConsumed()
    }

    data class LinkUICall(
        val email: String?,
        val phoneNumber: String?,
        val mode: NetworkedIdentityMode,
        val response: CompletableDeferred<Result<NetworkedIdentityLinkAccount?>>,
    )

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
}

internal fun niAccount(
    isVerified: Boolean = false,
    credentials: NetworkedIdentityCredentials? = NetworkedIdentityCredentials(
        publishableKey = "pk_consumer",
        sessionClientSecret = "session_secret",
    ),
) = NetworkedIdentityLinkAccount(
    email = "person@example.com",
    isVerified = isVerified,
    credentials = credentials,
)
