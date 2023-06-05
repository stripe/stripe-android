package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalError
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject

internal class PollAttachPaymentAccount @Inject constructor(
    private val repository: FinancialConnectionsAccountsRepository,
    private val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        allowManualEntry: Boolean,
        // null, when attaching via manual entry.
        activeInstitution: FinancialConnectionsInstitution?,
        // null, if account should not be saved to Link user.
        consumerSessionClientSecret: String?,
        params: PaymentAccountParams
    ): LinkAccountSessionPaymentAccount {
        return retryOnException(
            times = MAX_TRIES,
            delayMilliseconds = POLLING_TIME_MS,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            try {
                repository.postLinkAccountSessionPaymentAccount(
                    clientSecret = configuration.financialConnectionsSessionClientSecret,
                    paymentAccount = params,
                    consumerSessionClientSecret = consumerSessionClientSecret
                )
            } catch (
                @Suppress("SwallowedException") e: StripeException
            ) {
                throw e.toDomainException(
                    activeInstitution,
                    allowManualEntry
                )
            }
        }
    }

    private fun StripeException.toDomainException(
        institution: FinancialConnectionsInstitution?,
        allowManualEntry: Boolean
    ): StripeException =
        when {
            institution == null -> this
            stripeError?.extraFields?.get("reason") == "account_number_retrieval_failed" ->
                AccountNumberRetrievalError(
                    allowManualEntry = allowManualEntry,
                    institution = institution,
                    stripeException = this
                )
            else -> this
        }

    private companion object {
        private const val POLLING_TIME_MS = 250L
        private const val MAX_TRIES = 180
    }
}
