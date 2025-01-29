package com.stripe.android.link.account

import com.stripe.android.core.exception.APIException
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.APPLICATION_ID
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject
import javax.inject.Named

internal class DefaultLinkAuth @Inject constructor(
    private val linkGate: LinkGate,
    private val linkAccountManager: LinkAccountManager,
    private val integrityRequestManager: IntegrityRequestManager,
    @Named(APPLICATION_ID) private val applicationId: String
) : LinkAuth {
    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): LinkAuthResult {
        val signupResult = if (linkGate.useAttestationEndpoints) {
            mobileSignUp(
                email = email,
                phoneNumber = phoneNumber,
                country = country,
                name = name,
                consentAction = consentAction
            )
        } else {
            linkAccountManager.signUp(
                email = email,
                phone = phoneNumber,
                country = country,
                name = name,
                consentAction = consentAction
            )
        }
        return signupResult.toLinkAuthResult()
    }

    override suspend fun lookUp(email: String, emailSource: EmailSource): LinkAuthResult {
        val lookupResult = if (linkGate.useAttestationEndpoints) {
            mobileLookUp(
                email = email,
                emailSource = emailSource
            )
        } else {
            linkAccountManager.lookupConsumer(
                email = email,
                startSession = true
            )
        }
        return lookupResult.toLinkAuthResult()
    }

    private suspend fun mobileSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<LinkAccount> {
        return runCatching {
            val verificationToken = integrityRequestManager.requestToken().getOrThrow()
            linkAccountManager.mobileSignUp(
                email = email,
                phone = phoneNumber,
                country = country,
                name = name,
                consentAction = consentAction,
                verificationToken = verificationToken,
                appId = applicationId
            ).getOrThrow()
        }
    }

    private suspend fun mobileLookUp(
        email: String,
        emailSource: EmailSource
    ): Result<LinkAccount?> {
        return runCatching {
            val verificationToken = integrityRequestManager.requestToken().getOrThrow()
            linkAccountManager.mobileLookupConsumer(
                email = email,
                emailSource = emailSource,
                verificationToken = verificationToken,
                appId = applicationId,
                startSession = true
            ).getOrThrow()
        }
    }

    private fun Result<LinkAccount?>.toLinkAuthResult(): LinkAuthResult {
        return runCatching {
            val linkAccount = getOrThrow()
            return if (linkAccount != null) {
                LinkAuthResult.Success(linkAccount)
            } else {
                LinkAuthResult.NoLinkAccountFound
            }
        }.getOrElse { error ->
            error.toLinkAuthResult()
        }
    }

    private fun Throwable.toLinkAuthResult(): LinkAuthResult {
        return if (isAttestationError) {
            LinkAuthResult.AttestationFailed(this)
        } else {
            LinkAuthResult.Error(this)
        }
    }

    private val Throwable.isAttestationError: Boolean
        get() = when (this) {
            // Stripe backend could not verify the intregrity of the request
            is APIException -> stripeError?.code == "link_failed_to_attest_request"
            // Interaction with Integrity API to generate tokens resulted in a failure
            is AttestationError -> true
            else -> false
        }
}
