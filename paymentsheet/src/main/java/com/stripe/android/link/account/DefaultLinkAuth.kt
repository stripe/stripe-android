package com.stripe.android.link.account

import com.stripe.android.common.di.APPLICATION_ID
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkEventException
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject
import javax.inject.Named

/**
 * Default implementation of LinkAuth that handles low-level Link authentication operations.
 */
internal class DefaultLinkAuth @Inject constructor(
    private val linkGate: LinkGate,
    private val linkRepository: LinkRepository,
    private val integrityRequestManager: IntegrityRequestManager,
    private val errorReporter: ErrorReporter,
    private val config: LinkConfiguration,
    @Named(APPLICATION_ID) private val applicationId: String
) : LinkAuth {

    override suspend fun lookup(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        customerId: String?,
        sessionId: String,
        supportedVerificationTypes: List<String>?
    ): Result<ConsumerSessionLookup> {
        val hasEmailAndSource = email != null && emailSource != null
        val hasAuthIntent = linkAuthIntentId != null

        if (!hasEmailAndSource && !hasAuthIntent) {
            return Result.failure(
                IllegalArgumentException(
                    "Either email+emailSource or linkAuthIntentId must be provided"
                )
            )
        }
        return if (linkGate.useAttestationEndpoints) {
            mobileLookupWithAttestation(
                email = email,
                emailSource = emailSource,
                sessionId = sessionId,
                linkAuthIntentId = linkAuthIntentId,
                customerId = customerId,
                supportedVerificationTypes = supportedVerificationTypes
            )
        } else {
            linkRepository.lookupConsumer(
                email = email,
                linkAuthIntentId = linkAuthIntentId,
                sessionId = sessionId,
                customerId = customerId,
                supportedVerificationTypes = supportedVerificationTypes
            )
        }
    }

    override suspend fun signup(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: SignUpConsentAction,
    ): Result<ConsumerSessionSignup> {
        return if (linkGate.useAttestationEndpoints) {
            mobileSignUpWithAttestation(
                email = email,
                phoneNumber = phoneNumber,
                country = country,
                countryInferringMethod = countryInferringMethod,
                name = name,
                consentAction = consentAction
            )
        } else {
            linkRepository.consumerSignUp(
                email = email,
                phone = phoneNumber,
                country = country,
                countryInferringMethod = countryInferringMethod,
                name = name,
                consentAction = consentAction.consumerAction,
            )
        }
    }

    override suspend fun refreshConsumer(
        consumerSessionClientSecret: String,
        supportedVerificationTypes: List<String>?
    ): Result<ConsumerSessionRefresh> {
        return linkRepository.refreshConsumer(
            appId = applicationId,
            consumerSessionClientSecret = consumerSessionClientSecret,
            supportedVerificationTypes = supportedVerificationTypes
        )
    }

    private suspend fun mobileLookupWithAttestation(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        customerId: String?,
        sessionId: String,
        supportedVerificationTypes: List<String>?
    ): Result<ConsumerSessionLookup> {
        return runCatching {
            val verificationToken = integrityRequestManager.requestToken().getOrThrow()
            linkRepository.mobileLookupConsumer(
                verificationToken = verificationToken,
                appId = applicationId,
                email = email,
                linkAuthIntentId = linkAuthIntentId,
                sessionId = sessionId,
                emailSource = emailSource,
                customerId = customerId,
                supportedVerificationTypes = supportedVerificationTypes
            ).getOrThrow()
        }.onFailure { error ->
            val operation = if (email != null) "lookup" else "lookupByAuthIntent"
            reportAttestationError(error, operation = operation)
        }
    }

    private suspend fun mobileSignUpWithAttestation(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<ConsumerSessionSignup> {
        return runCatching {
            val verificationToken = integrityRequestManager.requestToken().getOrThrow()
            linkRepository.mobileSignUp(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                country = country,
                countryInferringMethod = countryInferringMethod,
                consentAction = consentAction.consumerAction,
                verificationToken = verificationToken,
                appId = applicationId,
                amount = config.stripeIntent.amount,
                currency = config.stripeIntent.currency,
                incentiveEligibilitySession = null,
            ).getOrThrow()
        }.onFailure { error ->
            reportAttestationError(error, operation = "signup")
        }
    }

    private fun reportAttestationError(error: Throwable, operation: String) {
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

    private val SignUpConsentAction.consumerAction: ConsumerSignUpConsentAction
        get() = when (this) {
            SignUpConsentAction.Checkbox ->
                ConsumerSignUpConsentAction.Checkbox
            SignUpConsentAction.CheckboxWithPrefilledEmail ->
                ConsumerSignUpConsentAction.CheckboxWithPrefilledEmail
            SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone ->
                ConsumerSignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
            SignUpConsentAction.Implied ->
                ConsumerSignUpConsentAction.Implied
            SignUpConsentAction.ImpliedWithPrefilledEmail ->
                ConsumerSignUpConsentAction.ImpliedWithPrefilledEmail
            SignUpConsentAction.DefaultOptInWithAllPrefilled ->
                ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledAll
            SignUpConsentAction.DefaultOptInWithSomePrefilled ->
                ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledSome
            SignUpConsentAction.DefaultOptInWithNonePrefilled ->
                ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledNone
            SignUpConsentAction.SignUpOptInMobileChecked ->
                ConsumerSignUpConsentAction.SignUpOptInMobileChecked
            SignUpConsentAction.SignUpOptInMobilePrechecked ->
                ConsumerSignUpConsentAction.SignUpOptInMobilePrechecked
        }
}
