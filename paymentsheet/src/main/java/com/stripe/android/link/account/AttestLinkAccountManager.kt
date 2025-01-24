package com.stripe.android.link.account

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.injection.APPLICATION_ID
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject
import javax.inject.Named

internal class AttestLinkAccountManager(
    private val config: LinkConfiguration,
    private val defaultLinkAccountManager: DefaultLinkAccountManager,
    private val integrityRequestManager: IntegrityRequestManager,
    private val linkRepository: LinkRepository,
    @Named(APPLICATION_ID) private val applicationId: String
) : LinkAccountManager by defaultLinkAccountManager {

    override suspend fun lookupConsumer(
        email: String,
        startSession: Boolean
    ) = runCatching {
        val tokenResult = integrityRequestManager.requestToken().getOrThrow()
        val lookupResult = linkRepository.mobileLookupConsumer(
            verificationToken = tokenResult,
            appId = applicationId,
            email = email,
            sessionId = config.elementSessionId,
            emailSource = EmailSource.USER_ACTION
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

    override suspend fun signUp(
        email: String,
        phone: String,
        country: String,
        name: String?,
        consentAction: SignUpConsentAction
    ) = runCatching {
        val tokenResult = integrityRequestManager.requestToken().getOrThrow()
        val consumerSessionSignUpResult = linkRepository.mobileSignUp(
            name = name,
            email = email,
            phoneNumber = phone,
            country = country,
            consentAction = consentAction.consumerAction,
            verificationToken = tokenResult,
            appId = applicationId,
            amount = config.stripeIntent.amount,
            currency = config.stripeIntent.currency,
            incentiveEligibilitySession = incentiveEligibilitySession(),
        )
        val consumerSessionSignUp = consumerSessionSignUpResult.getOrThrow()
        defaultLinkAccountManager.setAccount(
            consumerSession = consumerSessionSignUp.consumerSession,
            publishableKey = consumerSessionSignUp.publishableKey
        )
    }

    private fun incentiveEligibilitySession(): IncentiveEligibilitySession? {
        val id = config.stripeIntent.id ?: return null
        if (config.stripeIntent.clientSecret == null) {
            return IncentiveEligibilitySession.DeferredIntent(id)
        }
        return when (config.stripeIntent) {
            is PaymentIntent -> IncentiveEligibilitySession.PaymentIntent(id)
            is SetupIntent -> IncentiveEligibilitySession.SetupIntent(id)
        }
    }
}
