package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.utils.PollTimingOptions
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Polls accounts from backend after authorization session completes.
 *
 * Will retry upon 202 backend responses.
 */
internal class PollAuthorizationSessionAccounts @Inject constructor(
    private val repository: FinancialConnectionsAccountsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        canRetry: Boolean,
        manifest: FinancialConnectionsSessionManifest
    ): PartnerAccountsList = try {
        val activeAuthSession = requireNotNull(manifest.activeAuthSession)
        retryOnException(
            PollTimingOptions(
                initialDelayMs = activeAuthSession.flow.toPollIntervalMs(),
            ),
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            val accounts = repository.postAuthorizationSessionAccounts(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                sessionId = activeAuthSession.id
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
                accountsCount = stripeError?.extraFields?.get("total_accounts_count")?.toInt()
                    ?: 0,
                institution = institution,
                merchantName = businessName ?: "",
                stripeException = this
            )

        else -> AccountLoadError(
            allowManualEntry = allowManualEntry,
            institution = institution,
            canRetry = canRetry,
            stripeException = this
        )
    }

private fun FinancialConnectionsAuthorizationSession.Flow?.toPollIntervalMs(): Long {
    val defaultInitialPollDelay: Long = 1.75.seconds.inWholeMilliseconds
    return when (this) {
        FinancialConnectionsAuthorizationSession.Flow.TESTMODE,
        FinancialConnectionsAuthorizationSession.Flow.TESTMODE_OAUTH,
        FinancialConnectionsAuthorizationSession.Flow.TESTMODE_OAUTH_WEBVIEW,
        FinancialConnectionsAuthorizationSession.Flow.FINICITY_CONNECT_V2_LITE -> {
            // Post auth flow, Finicity non-OAuth account retrieval latency is extremely quick - p90 < 1sec.
            0
        }

        FinancialConnectionsAuthorizationSession.Flow.MX_CONNECT -> {
            // 10 account retrieval latency on MX non-OAuth sessions is currently 460 ms
            0.5.seconds.inWholeMilliseconds
        }

        else -> defaultInitialPollDelay
    }
}
