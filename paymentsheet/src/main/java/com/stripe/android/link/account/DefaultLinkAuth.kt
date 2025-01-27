package com.stripe.android.link.account

import com.stripe.android.core.exception.APIException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager

internal class DefaultLinkAuth constructor(
    private val linkConfiguration: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val integrityRequestManager: IntegrityRequestManager,
    private val applicationId: String
) : LinkAuth {
    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): LinkAuthResult {
        val signupResult = if (linkConfiguration.useAttestationEndpointsForLink) {
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
        val lookupResult = if (linkConfiguration.useAttestationEndpointsForLink) {
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
        return try {
            val linkAccount = getOrThrow()
            return if (linkAccount != null) {
                LinkAuthResult.Success(linkAccount)
            } else {
                LinkAuthResult.Error(Throwable("No link account found"))
            }
        } catch (e: Throwable) {
            e.toLinkAuthResult()
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