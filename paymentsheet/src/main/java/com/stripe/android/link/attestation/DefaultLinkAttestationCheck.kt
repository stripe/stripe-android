package com.stripe.android.link.attestation

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.account.toLinkAuthResult
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.EmailSource
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultLinkAttestationCheck @Inject constructor(
    private val linkGate: LinkGate,
    private val integrityRequestManager: IntegrityRequestManager,
    private val linkAccountManager: LinkAccountManager,
    private val linkConfiguration: LinkConfiguration,
    private val linkStore: LinkStore,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext
) : LinkAttestationCheck {
    override suspend fun invoke(): LinkAttestationCheck.Result {
        val shouldSkipAttestation = linkStore.hasPassedAttestationChecksRecently()
        if (linkGate.useAttestationEndpoints.not()) return LinkAttestationCheck.Result.Successful
        if (shouldSkipAttestation) return LinkAttestationCheck.Result.Successful

        return withContext(workContext) {
            val result = integrityRequestManager.prepare()
            result.fold(
                onSuccess = {
                    val email = linkAccountManager.linkAccountInfo.value.account?.email
                        ?: linkConfiguration.customerInfo.email
                    if (email == null) return@fold LinkAttestationCheck.Result.Successful
                    val lookupResult = linkAccountManager.lookupByEmail(
                        email = email,
                        emailSource = EmailSource.CUSTOMER_OBJECT,
                        startSession = false,
                        customerId = linkConfiguration.customerIdForEceDefaultValues
                    )
                    val attestationResult = handleLookupResult(lookupResult)
                    if (attestationResult is LinkAttestationCheck.Result.Successful) {
                        linkStore.markAttestationCheckAsPassed()
                    }
                    attestationResult
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

    private fun handleLookupResult(lookupResult: Result<LinkAccount?>): LinkAttestationCheck.Result {
        return when (val result = lookupResult.toLinkAuthResult()) {
            is LinkAuthResult.Success -> LinkAttestationCheck.Result.Successful
            is LinkAuthResult.NoLinkAccountFound -> LinkAttestationCheck.Result.Successful
            is LinkAuthResult.AttestationFailed -> LinkAttestationCheck.Result.AttestationFailed(result.error)
            is LinkAuthResult.AccountError -> LinkAttestationCheck.Result.AccountError(result.error)
            is LinkAuthResult.Error -> LinkAttestationCheck.Result.Error(result.error)
        }
    }
}
