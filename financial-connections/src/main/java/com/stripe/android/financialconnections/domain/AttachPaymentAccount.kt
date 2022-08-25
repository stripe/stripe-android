package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import javax.inject.Inject

internal class AttachPaymentAccount @Inject constructor(
    val repository: FinancialConnectionsAccountsRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    suspend operator fun invoke(
        params: PaymentAccountParams
    ): LinkAccountSessionPaymentAccount = repository.postLinkAccountSessionPaymentAccount(
        clientSecret = configuration.financialConnectionsSessionClientSecret,
        paymentAccount = params
    )
}
