package com.stripe.android.link.attestation

import com.stripe.android.core.exception.StripeException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.model.EmailSource
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject

internal class DefaultLinkAttestationCheck @Inject constructor(
    private val linkGate: LinkGate,
    private val linkAuth: LinkAuth,
    private val integrityRequestManager: IntegrityRequestManager,
    private val linkAccountManager: LinkAccountManager,
    private val linkConfiguration: LinkConfiguration,
    private val errorReporter: ErrorReporter
) : LinkAttestationCheck {
    override suspend fun invoke(): LinkAttestationCheck.Result {
        if (linkGate.useAttestationEndpoints.not()) return LinkAttestationCheck.Result.Successful
        val result = integrityRequestManager.prepare()

        return result.fold(
            onSuccess = {
                val email = linkAccountManager.linkAccount.value?.email
                    ?: linkConfiguration.customerInfo.email
                if (email == null) return@fold LinkAttestationCheck.Result.Successful
                val lookupResult = linkAuth.lookUp(
                    email = email,
                    emailSource = EmailSource.CUSTOMER_OBJECT,
                    startSession = false
                )
                when (lookupResult) {
                    is LinkAuthResult.AttestationFailed -> {
                        LinkAttestationCheck.Result.AttestationFailed(lookupResult.error)
                    }
                    is LinkAuthResult.Error -> {
                        LinkAttestationCheck.Result.Error(lookupResult.error)
                    }
                    is LinkAuthResult.AccountError -> {
                        LinkAttestationCheck.Result.AccountError(lookupResult.error)
                    }
                    LinkAuthResult.NoLinkAccountFound,
                    is LinkAuthResult.Success -> {
                        LinkAttestationCheck.Result.Successful
                    }
                }
            },
            onFailure = { error ->
                errorReporter.report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_PREPARE_INTEGRITY_MANAGER,
                    stripeException = StripeException.create(error)
                )
                LinkAttestationCheck.Result.AttestationFailed(error)
            }
        )
    }
}
