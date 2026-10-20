@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.identity.networked

import android.app.Application
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkControllerPreview

/**
 * [NetworkedIdentityLinkSession] backed by [LinkController]. Only [authenticateWithLinkUI] presents Link UI.
 *
 * Kept as a thin mapping: [LinkController] can't be faked, so behavior is tested through
 * [NetworkedIdentityLinkSession] fakes and Link's own tests.
 */
internal class LinkControllerNetworkedIdentityLinkSession(
    private val application: Application,
    private val linkHost: NetworkedIdentityLinkHost,
) : NetworkedIdentityLinkSession {
    private val linkController: LinkController
        get() = linkHost.linkController

    override suspend fun configure(merchantPublishableKey: String, merchantDisplayName: String): Result<Unit> {
        return linkController.configure(
            LinkController.Configuration(
                merchantDisplayName = merchantDisplayName,
                publishableKey = merchantPublishableKey,
            )
        )
    }

    override suspend fun authenticateWithLinkUI(
        email: String?,
        phoneNumber: String?,
        mode: NetworkedIdentityMode,
    ): Result<NetworkedIdentityLinkAccount?> {
        // #TODO - Networked Identity: localize once the final mobile copy is approved.
        val content = LinkController.AuthenticationContent(
            title = "Continue with Link",
            subtitle = when (mode) {
                NetworkedIdentityMode.Save -> "Sign in or create an account to save ID."
                NetworkedIdentityMode.Reuse -> "Sign in to use a saved ID."
            },
            consentAction = LinkController.AuthenticationConsentAction.NetworkedIdentity,
        )
        return when (val result = linkHost.authenticate(email, phoneNumber, content)) {
            LinkController.AuthenticationResult.Success -> currentAccount()
            LinkController.AuthenticationResult.Canceled -> Result.success(null)
            is LinkController.AuthenticationResult.Failed -> Result.failure(result.error)
            null -> Result.failure(IllegalStateException("No activity can present Link."))
        }
    }

    private fun currentAccount(): Result<NetworkedIdentityLinkAccount> {
        val account = linkController.state(application).value.internalLinkAccount
            ?: return Result.failure(IllegalStateException("Link returned no account."))
        return Result.success(
            NetworkedIdentityLinkAccount(
                email = account.email,
                isVerified = account.sessionState == LinkController.SessionState.LoggedIn,
                credentials = account.linkSessionKey?.takeIf { it.isNotBlank() }?.let(::NetworkedIdentityCredentials),
            )
        )
    }
}
