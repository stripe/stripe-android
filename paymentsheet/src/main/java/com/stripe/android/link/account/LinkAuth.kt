package com.stripe.android.link.account

import com.stripe.android.common.di.APPLICATION_ID
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.LinkEventException
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.EmailSource
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject
import javax.inject.Named

/**
 *
 * This class handles the low-level authentication operations, deciding between regular
 * and mobile endpoints based on attestation settings.
 */
internal class LinkAuth @Inject constructor(
    private val linkGate: LinkGate,
    private val linkRepository: LinkRepository,
    private val integrityRequestManager: IntegrityRequestManager,
    private val errorReporter: ErrorReporter,
    @Named(APPLICATION_ID) private val applicationId: String
) {
    suspend fun lookup(
        email: String? = null,
        emailSource: EmailSource? = null,
        linkAuthIntentId: String? = null,
        customerId: String? = null
    ): Result<ConsumerSessionLookup> {
        return if (linkGate.useAttestationEndpoints) {
            when {
                email != null && emailSource != null -> {
                    mobileLookupWithAttestation(
                        email = email,
                        emailSource = emailSource,
                        linkAuthIntentId = null,
                        customerId = customerId
                    )
                }
                linkAuthIntentId != null -> {
                    mobileLookupWithAttestation(
                        email = null,
                        emailSource = null,
                        linkAuthIntentId = linkAuthIntentId,
                        customerId = customerId
                    )
                }
                else -> Result.failure(IllegalArgumentException("Either email or linkAuthIntentId must be provided"))
            }
        } else {
            linkRepository.lookupConsumer(
                email = email,
                linkAuthIntentId = linkAuthIntentId,
                sessionId = "",
                customerId = customerId
            )
        }
    }

    suspend fun signup(
        email: String,
        phoneNumber: String?,
        country: String?,
        countryInferringMethod: String,
        name: String?,
        consentAction: SignUpConsentAction
    ): Result<ConsumerSessionSignup> {
        return if (linkGate.useAttestationEndpoints) {
            mobileSignUpWithAttestation(
                email = email,
                phoneNumber = requireNotNull(phoneNumber, { "Phone number is required for mobile signup" }),
                country = requireNotNull(country, { "Country is required for mobile signup" }),
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
                consentAction = consentAction.consumerAction
            )
        }
    }

    private suspend fun mobileLookupWithAttestation(
        email: String?,
        emailSource: EmailSource?,
        linkAuthIntentId: String?,
        customerId: String?
    ): Result<ConsumerSessionLookup> {
        return runCatching {
            val verificationToken = integrityRequestManager.requestToken().getOrThrow()
            linkRepository.mobileLookupConsumer(
                verificationToken = verificationToken,
                appId = applicationId,
                email = email,
                linkAuthIntentId = linkAuthIntentId,
                sessionId = "",
                emailSource = emailSource,
                customerId = customerId
            ).getOrThrow()
        }.onFailure { error ->
            val operation = if (email != null) "lookup" else "lookupByAuthIntent"
            reportAttestationError(error, operation = operation)
        }
    }

    private suspend fun mobileSignUpWithAttestation(
        email: String,
        phoneNumber: String,
        country: String,
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
                amount = null,
                currency = null,
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

    private val Throwable.isIntegrityManagerError: Boolean
        get() = this is AttestationError

    private val Throwable.isBackendAttestationError: Boolean
        get() = this is APIException && stripeError?.code == "link_failed_to_attest_request"

    private val SignUpConsentAction.consumerAction: com.stripe.android.model.ConsumerSignUpConsentAction
        get() = when (this) {
            SignUpConsentAction.Checkbox ->
                com.stripe.android.model.ConsumerSignUpConsentAction.Checkbox
            SignUpConsentAction.CheckboxWithPrefilledEmail ->
                com.stripe.android.model.ConsumerSignUpConsentAction.CheckboxWithPrefilledEmail
            SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone ->
                com.stripe.android.model.ConsumerSignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
            SignUpConsentAction.Implied ->
                com.stripe.android.model.ConsumerSignUpConsentAction.Implied
            SignUpConsentAction.ImpliedWithPrefilledEmail ->
                com.stripe.android.model.ConsumerSignUpConsentAction.ImpliedWithPrefilledEmail
            SignUpConsentAction.DefaultOptInWithAllPrefilled ->
                com.stripe.android.model.ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledAll
            SignUpConsentAction.DefaultOptInWithSomePrefilled ->
                com.stripe.android.model.ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledSome
            SignUpConsentAction.DefaultOptInWithNonePrefilled ->
                com.stripe.android.model.ConsumerSignUpConsentAction.PrecheckedOptInBoxPrefilledNone
            SignUpConsentAction.SignUpOptInMobileChecked ->
                com.stripe.android.model.ConsumerSignUpConsentAction.SignUpOptInMobileChecked
            SignUpConsentAction.SignUpOptInMobilePrechecked ->
                com.stripe.android.model.ConsumerSignUpConsentAction.SignUpOptInMobilePrechecked
        }
}
