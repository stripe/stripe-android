package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.NoSupportedPaymentMethodTypeAccountsException
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject

/**
 * Polls accounts from backend after authorization session completes.
 *
 * Will retry upon 202 backend responses every [POLLING_TIME_MS] up to [MAX_TRIES]
 */
internal class PollAuthorizationSessionAccounts @Inject constructor(
    private val repository: FinancialConnectionsAccountsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        manifest: FinancialConnectionsSessionManifest,
    ): PartnerAccountsList {
        return retryOnException(
            times = MAX_TRIES,
            delayMilliseconds = POLLING_TIME_MS,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            try {
                repository.postAuthorizationSessionAccounts(
                    clientSecret = configuration.financialConnectionsSessionClientSecret,
                    sessionId = manifest.activeAuthSession!!.id
                )
            } catch (@Suppress("SwallowedException") e: StripeException) {
                throw e.toDomainException(
                    requireNotNull(manifest.activeInstitution),
                    ConsentTextBuilder.getBusinessName(manifest)
                )
            }
        }
    }

    private fun StripeException.toDomainException(
        institution: FinancialConnectionsInstitution,
        businessName: String?
    ): StripeException = this.stripeError?.let {
        return if (it.extraFields?.get("reason") == "no_supported_payment_method_type_accounts_found") {
            val accountsCount = it.extraFields?.get("total_accounts_count")?.toInt() ?: 0
            NoSupportedPaymentMethodTypeAccountsException(
                accountsCount = accountsCount,
                institution = institution,
                stripeException = this,
                merchantName = businessName ?: ""
            )
        } else this
    } ?: this

    private companion object {
        private const val POLLING_TIME_MS = 2000L
        private const val MAX_TRIES = 10
    }
}
