package com.stripe.android.link.attestation

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
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
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext
) : LinkAttestationCheck {
    override suspend fun invoke(): LinkAttestationCheck.Result {
        if (linkGate.useAttestationEndpoints.not()) return LinkAttestationCheck.Result.Successful
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
                    handleLookupResult(lookupResult)
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
        return lookupResult.fold(
            onSuccess = {
                // Both successful lookup (with account) and no account found are considered successful
                LinkAttestationCheck.Result.Successful
            },
            onFailure = { error ->
                // Map error types based on the original LinkAuth error handling logic
                when {
                    isAttestationError(error) -> {
                        LinkAttestationCheck.Result.AttestationFailed(error)
                    }
                    isAccountError(error) -> {
                        LinkAttestationCheck.Result.AccountError(error)
                    }
                    else -> {
                        LinkAttestationCheck.Result.Error(error)
                    }
                }
            }
        )
    }

    private fun isAttestationError(error: Throwable): Boolean {
        return error is com.stripe.attestation.AttestationError ||
            (
                error is com.stripe.android.core.exception.APIException &&
                    error.stripeError?.code == "link_failed_to_attest_request"
                )
    }

    private fun isAccountError(error: Throwable): Boolean {
        return error is com.stripe.android.core.exception.APIException &&
            error.stripeError?.code == "link_consumer_details_not_available"
    }
}
