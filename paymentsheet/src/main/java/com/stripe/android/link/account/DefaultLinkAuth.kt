package com.stripe.android.link.account

import com.stripe.android.common.di.APPLICATION_ID
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.LinkEventException
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject
import javax.inject.Named

internal class DefaultLinkAuth @Inject constructor(
    private val linkGate: LinkGate,
    private val linkAccountManager: LinkAccountManager,
    private val integrityRequestManager: IntegrityRequestManager,
    private val errorReporter: ErrorReporter,
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

    override suspend fun lookUp(
        email: String,
        emailSource: EmailSource,
        startSession: Boolean
    ): LinkAuthResult {
        val lookupResult = if (linkGate.useAttestationEndpoints) {
            mobileLookUp(
                email = email,
                emailSource = emailSource,
                startSession = startSession
            )
        } else {
            linkAccountManager.lookupConsumer(
                email = email,
                emailSource = emailSource,
                startSession = startSession
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
        }.onFailure { error ->
            reportError(error, operation = "signup")
        }
    }

    private suspend fun mobileLookUp(
        email: String,
        emailSource: EmailSource,
        startSession: Boolean
    ): Result<LinkAccount?> {
        return runCatching {
            val verificationToken = integrityRequestManager.requestToken().getOrThrow()
            linkAccountManager.mobileLookupConsumer(
                email = email,
                emailSource = emailSource,
                verificationToken = verificationToken,
                appId = applicationId,
                startSession = startSession
            ).getOrThrow()
        }.onFailure { error ->
            reportError(error, operation = "lookup")
        }
    }

    private fun reportError(error: Throwable, operation: String) {
        val errorEvent = when {
            error.isBackendAttestationError -> {
                ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_ATTEST_REQUEST
            }
            error.isIntegrityManagerError -> {
                ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_GET_INTEGRITY_TOKEN
            }
            else -> return
        }
        errorReporter.report(
            errorEvent = errorEvent,
            stripeException = LinkEventException(error),
            additionalNonPiiParams = mapOf(
                "operation" to operation
            )
        )
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
        return when {
            isAttestationError -> {
                LinkAuthResult.AttestationFailed(this)
            }
            isAccountError -> {
                LinkAuthResult.AccountError(this)
            }
            else -> {
                LinkAuthResult.Error(this)
            }
        }
    }

    private val Throwable.isAttestationError: Boolean
        get() = isIntegrityManagerError || isBackendAttestationError

    // Interaction with Integrity API to generate tokens resulted in a failure
    private val Throwable.isIntegrityManagerError: Boolean
        get() = this is AttestationError

    // Stripe backend could not verify the integrity of the request
    private val Throwable.isBackendAttestationError: Boolean
        get() = this is APIException && stripeError?.code == "link_failed_to_attest_request"

    private val Throwable.isAccountError: Boolean
        get() = when (this) {
            // This happens when account is suspended or banned
            is APIException -> stripeError?.code == "link_consumer_details_not_available"
            else -> false
        }
}
