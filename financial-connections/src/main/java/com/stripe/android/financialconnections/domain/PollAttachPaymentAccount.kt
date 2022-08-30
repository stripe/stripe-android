package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.utils.retryOnException
import com.stripe.android.financialconnections.utils.shouldRetry
import javax.inject.Inject

internal class PollAttachPaymentAccount @Inject constructor(
    val repository: FinancialConnectionsAccountsRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        params: PaymentAccountParams
    ): LinkAccountSessionPaymentAccount {
        return retryOnException(
            times = MAX_TRIES,
            delayMilliseconds = POLLING_TIME_MS,
            retryCondition = { exception -> exception.shouldRetry }
        ) {
            repository.postLinkAccountSessionPaymentAccount(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                paymentAccount = params
            )
        }
    }

    private companion object {
        private const val POLLING_TIME_MS = 250L
        private const val MAX_TRIES = 180
    }
}
