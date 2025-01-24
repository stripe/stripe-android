package com.stripe.android.link.account

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.confirmation.Result
import com.stripe.android.link.injection.APPLICATION_ID
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject
import javax.inject.Named

internal class AttestLinkAccountManager @Inject constructor(
    private val config: LinkConfiguration,
    private val defaultLinkAccountManager: DefaultLinkAccountManager,
    private val integrityRequestManager: IntegrityRequestManager,
    private val linkRepository: LinkRepository,
    @Named(APPLICATION_ID) private val applicationId: String
): LinkAccountManager by defaultLinkAccountManager {

    init {

    }

    override suspend fun lookupConsumer(
        email: String,
        startSession: Boolean
    ): Result<LinkAccount?> {
        return runCatching {
            val tokenResult = integrityRequestManager.requestToken().getOrThrow()
            val lookupResult = linkRepository.mobileLookupConsumer(
                verificationToken = tokenResult,
                appId = applicationId,
                email = email,
                sessionId = config.elementSessionId?.takeIf { it.isNotBlank() } ?: throw NO_ELEMENT_SESSION_ID
            )
            val lookup = lookupResult.getOrThrow()
            val linkAccount = lookup.consumerSession?.let { LinkAccount(it) }

            if (startSession) {
                defaultLinkAccountManager.setAccountNullable(
                    consumerSession = lookup.consumerSession,
                    publishableKey = lookup.publishableKey
                )
            }
            linkAccount
        }
    }

    override suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        return runCatching {
            val tokenResult = integrityRequestManager.requestToken().getOrThrow()
            val consumerSessionSignUpResult = linkRepository.mobileSignUp(
                name = name,
                email = email,
                phoneNumber = phone,
                country = country,
                consentAction = consentAction.consumerAction,
                verificationToken = tokenResult,
                appId = applicationId
            )
            val consumerSessionSignUp = consumerSessionSignUpResult.getOrThrow()
            defaultLinkAccountManager.setAccount(
                consumerSession = consumerSessionSignUp.consumerSession,
                publishableKey = consumerSessionSignUp.publishableKey
            )
        }
    }

}