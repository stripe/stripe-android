package com.stripe.android.identity.networked

import app.cash.turbine.Turbine
import kotlinx.coroutines.CompletableDeferred

internal class FakeNetworkedIdentityLinkSession : NetworkedIdentityLinkSession {
    val configureCalls = Turbine<ConfigureCall>()
    val linkUICalls = Turbine<LinkUICall>()

    override suspend fun configure(merchantPublishableKey: String, merchantDisplayName: String): Result<Unit> {
        val call = ConfigureCall(merchantPublishableKey, merchantDisplayName, CompletableDeferred())
        configureCalls.add(call)
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
}

internal fun niAccount(
    isVerified: Boolean = false,
    credentials: NetworkedIdentityCredentials? = NetworkedIdentityCredentials(linkSessionKey = "lsk_session"),
) = NetworkedIdentityLinkAccount(
    email = "person@example.com",
    isVerified = isVerified,
    credentials = credentials,
)
