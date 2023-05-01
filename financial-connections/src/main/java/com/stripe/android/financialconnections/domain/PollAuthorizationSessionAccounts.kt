package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.common.getBusinessName
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
        canRetry: Boolean,
        manifest: FinancialConnectionsSessionManifest
    ): PartnerAccountsList {
        return retryOnException(
            times = MAX_TRIES,
            delayMilliseconds = POLLING_TIME_MS,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            try {
                val authSession = requireNotNull(manifest.activeAuthSession)
                val accounts = repository.postAuthorizationSessionAccounts(
                    clientSecret = configuration.financialConnectionsSessionClientSecret,
                    sessionId = authSession.id
                )
                if (accounts.data.isEmpty()) {
                    throw AccountLoadError(
                        institution = requireNotNull(manifest.activeInstitution),
                        allowManualEntry = manifest.allowManualEntry,
                        canRetry = canRetry,
                        stripeException = APIException()
                    )
                } else {
                    accounts
                }
            } catch (@Suppress("SwallowedException") e: StripeException) {
                throw e.toDomainException(
                    institution = manifest.activeInstitution,
                    businessName = manifest.getBusinessName(),
                    canRetry = canRetry,
                    allowManualEntry = manifest.allowManualEntry
                )
            }
        }
    }

    private fun StripeException.toDomainException(
        institution: FinancialConnectionsInstitution?,
        businessName: String?,
        canRetry: Boolean,
        allowManualEntry: Boolean
    ): StripeException =
        when {
            institution == null -> this
            stripeError?.extraFields?.get("reason") == "no_supported_payment_method_type_accounts_found" ->
                AccountNoneEligibleForPaymentMethodError(
                    allowManualEntry = allowManualEntry,
                    accountsCount = stripeError?.extraFields?.get("total_accounts_count")?.toInt()
                        ?: 0,
                    institution = institution,
                    stripeException = this,
                    merchantName = businessName ?: ""
                )

            else -> AccountLoadError(
                allowManualEntry = allowManualEntry,
                institution = institution,
                canRetry = canRetry,
                stripeException = this
            )
        }

    private companion object {
        private const val POLLING_TIME_MS = 2000L
        private const val MAX_TRIES = 10
    }
}
