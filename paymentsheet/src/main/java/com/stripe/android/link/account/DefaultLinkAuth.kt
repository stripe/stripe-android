package com.stripe.android.link.account

import com.stripe.android.core.exception.APIException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.attestation.AttestationError
import javax.inject.Inject

internal class DefaultLinkAuth @Inject constructor(
    private val linkConfiguration: LinkConfiguration,
    private val defaultLinkAccountManager: DefaultLinkAccountManager,
    private val attestLinkAccountManager: AttestLinkAccountManager,
) : LinkAuth {

    @SuppressWarnings("TooGenericExceptionCaught")
    override suspend fun signUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): LinkAuthResult {
        val consumerSessionSignUpResult = if (linkConfiguration.useAttestationEndpointsForLink) {
            attestLinkAccountManager.signUp(
                name = name,
                email = email,
                phone = phoneNumber,
                country = country,
                consentAction = consentAction,
            )
        } else {
            defaultLinkAccountManager.signUp(
                name = name,
                email = email,
                phone = phoneNumber,
                country = country,
                consentAction = consentAction,
            )
        }
        return consumerSessionSignUpResult.toLinkAuthResult()
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    override suspend fun lookUp(email: String): LinkAuthResult {
        val lookupResult = if (linkConfiguration.useAttestationEndpointsForLink) {
            attestLinkAccountManager.lookupConsumer(email)
        } else {
            defaultLinkAccountManager.lookupConsumer(email)
        }

        return lookupResult.toLinkAuthResult()
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

    companion object {
        internal val NO_ELEMENT_SESSION_ID = IllegalStateException("No element session id found.")
    }
}
