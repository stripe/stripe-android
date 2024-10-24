package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.launcher.InstantDebitsResult
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.model.ConsumerPaymentDetails.BankAccount
import com.stripe.android.model.LinkMode
import javax.inject.Inject

internal fun interface CreateInstantDebitsResult {
    suspend operator fun invoke(
        bankAccountId: String,
    ): InstantDebitsResult
}

internal class RealCreateInstantDebitsResult @Inject constructor(
    private val consumerRepository: FinancialConnectionsConsumerSessionRepository,
    private val repository: FinancialConnectionsRepository,
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val elementsSessionContext: ElementsSessionContext?,
) : CreateInstantDebitsResult {

    override suspend fun invoke(
        bankAccountId: String,
    ): InstantDebitsResult {
        val consumerSession = consumerSessionProvider.provideConsumerSession()

        val clientSecret = requireNotNull(consumerSession?.clientSecret) {
            "Consumer session client secret cannot be null"
        }

        val billingDetails = elementsSessionContext?.billingDetails

        val response = consumerRepository.createPaymentDetails(
            consumerSessionClientSecret = clientSecret,
            bankAccountId = bankAccountId,
            billingDetails = billingDetails,
        )

        val paymentDetails = response.paymentDetails.filterIsInstance<BankAccount>().first()

        val paymentMethodId = if (elementsSessionContext?.linkMode == LinkMode.LinkCardBrand) {
            consumerRepository.sharePaymentDetails(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = clientSecret,
                expectedPaymentMethodType = elementsSessionContext.linkMode.expectedPaymentMethodType,
                billingPhone = elementsSessionContext.billingDetails?.phone,
            ).paymentMethodId
        } else {
            repository.createPaymentMethod(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = clientSecret,
                billingDetails = billingDetails,
            ).id
        }

        return InstantDebitsResult(
            paymentMethodId = paymentMethodId,
            bankName = paymentDetails.bankName,
            last4 = paymentDetails.last4,
        )
    }
}
