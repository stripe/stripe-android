package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalError
import com.stripe.android.financialconnections.features.common.showManualEntryInErrors
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.utils.PollTimingOptions
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class PollAttachPaymentAccount @Inject constructor(
    private val repository: FinancialConnectionsAccountsRepository,
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val attachedPaymentAccountRepository: AttachedPaymentAccountRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        sync: SynchronizeSessionResponse,
        // null, when attaching via manual entry.
        activeInstitution: FinancialConnectionsInstitution?,
        params: PaymentAccountParams
    ): LinkAccountSessionPaymentAccount {
        return retryOnException(
            PollTimingOptions(
                initialDelayMs = 1.seconds.inWholeMilliseconds,
            ),
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            try {
                repository.postAttachPaymentAccountToLinkAccountSession(
                    clientSecret = configuration.financialConnectionsSessionClientSecret,
                    paymentAccount = params,
                    // null, if account should not be saved to Link user.
                    consumerSessionClientSecret = consumerSessionProvider.provideConsumerSession()?.clientSecret,
                ).also {
                    attachedPaymentAccountRepository.set(params)
                }
            } catch (e: StripeException) {
                throw e.toDomainException(
                    activeInstitution,
                    sync.showManualEntryInErrors()
                )
            }
        }
    }

    private fun StripeException.toDomainException(
        institution: FinancialConnectionsInstitution?,
        showManualEntry: Boolean
    ): StripeException =
        when {
            institution == null -> this
            stripeError?.extraFields?.get("reason") == "account_number_retrieval_failed" ->
                AccountNumberRetrievalError(
                    showManualEntry = showManualEntry,
                    institution = institution,
                    stripeException = this
                )

            else -> this
        }
}
