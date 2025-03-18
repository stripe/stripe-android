package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.exception.AccountLoadError
import com.stripe.android.financialconnections.exception.AccountNoneEligibleForPaymentMethodError
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.features.common.showManualEntryInErrors
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession.Flow
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
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
    private val configuration: FinancialConnectionsSheetConfiguration
) {

    suspend operator fun invoke(
        canRetry: Boolean,
        sync: SynchronizeSessionResponse
    ): PartnerAccountsList = try {
        val manifest = requireNotNull(sync.manifest)
        val activeAuthSession = requireNotNull(manifest.activeAuthSession)
        retryOnException(
            PollTimingOptions(
                initialDelayMs = Flow.entries
                    .firstOrNull { it.value == activeAuthSession.flow }.toPollIntervalMs(),
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
                    showManualEntry = sync.showManualEntryInErrors(),
                    canRetry = canRetry,
                    stripeException = APIException()
                )
            } else {
                accounts
            }
        }
    } catch (e: StripeException) {
        throw e.toDomainException(
            institution = sync.manifest.activeInstitution,
            businessName = sync.manifest.getBusinessName(),
            canRetry = canRetry,
            showManualEntry = sync.showManualEntryInErrors()
        )
    }
}

private fun StripeException.toDomainException(
    institution: FinancialConnectionsInstitution?,
    businessName: String?,
    canRetry: Boolean,
    showManualEntry: Boolean
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
            showManualEntry = showManualEntry,
            institution = institution,
            canRetry = canRetry,
            stripeException = this
        )
    }

private fun Flow?.toPollIntervalMs(): Long {
    val defaultInitialPollDelay: Long = 1.75.seconds.inWholeMilliseconds
    return when (this) {
        Flow.TESTMODE,
        Flow.TESTMODE_OAUTH,
        Flow.TESTMODE_OAUTH_WEBVIEW,
        Flow.FINICITY_CONNECT_V2_LITE -> {
            // Post auth flow, Finicity non-OAuth account retrieval latency is extremely quick - p90 < 1sec.
            0
        }

        Flow.MX_CONNECT -> {
            // 10 account retrieval latency on MX non-OAuth sessions is currently 460 ms
            0.5.seconds.inWholeMilliseconds
        }

        else -> defaultInitialPollDelay
    }
}
