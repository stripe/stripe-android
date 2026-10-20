@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.identity.networked

import android.app.Application
import com.stripe.android.identity.R
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
        val content = LinkController.AuthenticationContent(
            title = application.getString(R.string.stripe_identity_link_continue_with_link),
            subtitle = application.getString(
                when (mode) {
                    NetworkedIdentityMode.Save -> R.string.stripe_identity_link_sign_in_to_save
                    NetworkedIdentityMode.Reuse -> R.string.stripe_identity_link_sign_in_to_reuse
                }
            ),
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
