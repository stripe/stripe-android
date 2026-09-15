@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.identity.networked

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.networking.RequestSurface

/**
 * [NetworkedIdentityLinkSession] backed by [LinkController], without presenting any Link UI.
 *
 * Kept as a thin mapping: [LinkController] can't be faked, so behavior is tested through
 * [NetworkedIdentityLinkSession] fakes and Link's own tests.
 */
internal class LinkControllerNetworkedIdentityLinkSession(
    private val application: Application,
    private val linkController: LinkController,
) : NetworkedIdentityLinkSession {

    override suspend fun configure(merchantPublishableKey: String, merchantDisplayName: String): Result<Unit> {
        return linkController.configure(
            LinkController.Configuration(
                merchantDisplayName = merchantDisplayName,
                publishableKey = merchantPublishableKey,
            )
        )
    }

    override suspend fun restore(credentials: NetworkedIdentityCredentials): Result<NetworkedIdentityLinkAccount> {
        return when (
            val result = linkController.restoreConsumerSession(
                consumerSessionClientSecret = credentials.sessionClientSecret,
                consumerPublishableKey = credentials.publishableKey,
            )
        ) {
            LinkController.RestoreConsumerSessionResult.Success -> currentAccount()
            is LinkController.RestoreConsumerSessionResult.Failed -> Result.failure(result.error)
        }
    }

    override suspend fun lookup(email: String): Result<NetworkedIdentityLinkAccount?> {
        return when (val result = linkController.lookupConsumer(email)) {
            is LinkController.LookupConsumerResult.Success ->
                if (result.isConsumer) currentAccount() else Result.success(null)
            is LinkController.LookupConsumerResult.Failed -> Result.failure(result.error)
        }
    }

    override suspend fun startVerification(isResend: Boolean): Result<NetworkedIdentityLinkAccount> {
        return when (val result = linkController.startVerification(isResendSmsCode = isResend)) {
            LinkController.StartVerificationResult.Success -> currentAccount()
            is LinkController.StartVerificationResult.Failed -> Result.failure(result.error)
        }
    }

    override suspend fun confirmVerification(code: String): Result<NetworkedIdentityLinkAccount> {
        return when (val result = linkController.confirmVerification(code)) {
            LinkController.ConfirmVerificationResult.Success -> currentAccount()
            is LinkController.ConfirmVerificationResult.Failed -> Result.failure(result.error)
        }
    }

    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
    ): Result<NetworkedIdentityLinkAccount> {
        val result = linkController.registerConsumer(
            email = email,
            phone = phoneNumber,
            country = country,
            name = name,
            consentAction = LinkController.RegisterConsumerConsentAction.NetworkedIdentity,
        )
        return when (result) {
            LinkController.RegisterConsumerResult.Success -> currentAccount()
            is LinkController.RegisterConsumerResult.Failed -> Result.failure(result.error)
        }
    }

    override suspend fun logOut(): Result<Unit> {
        return when (val result = linkController.logOut()) {
            is LinkController.LogOutResult.Success -> Result.success(Unit)
            is LinkController.LogOutResult.Failed -> Result.failure(result.error)
        }
    }

    private fun currentAccount(): Result<NetworkedIdentityLinkAccount> {
        val account = linkController.state(application).value.internalLinkAccount
            ?: return Result.failure(IllegalStateException("Link returned no account."))
        val secret = account.consumerSessionClientSecret
        val publishableKey = account.consumerPublishableKey
        return Result.success(
            NetworkedIdentityLinkAccount(
                email = account.email,
                redactedPhoneNumber = account.redactedPhoneNumber,
                isVerified = account.sessionState == LinkController.SessionState.LoggedIn,
                credentials = if (secret.isNullOrBlank() || publishableKey.isNullOrBlank()) {
                    null
                } else {
                    NetworkedIdentityCredentials(publishableKey = publishableKey, sessionClientSecret = secret)
                },
            )
        )
    }

    internal companion object {
        fun create(
            application: Application,
            savedStateHandle: SavedStateHandle,
        ): LinkControllerNetworkedIdentityLinkSession = LinkControllerNetworkedIdentityLinkSession(
            application = application,
            linkController = LinkController.create(
                application = application,
                savedStateHandle = savedStateHandle,
                requestSurface = RequestSurface.Identity,
            ),
        )
    }
}
