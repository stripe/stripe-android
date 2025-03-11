package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.ElementsSessionContext
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

        val paymentMethod = if (elementsSessionContext?.linkMode == LinkMode.LinkCardBrand) {
            val sharePaymentDetails = consumerRepository.sharePaymentDetails(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = clientSecret,
                expectedPaymentMethodType = elementsSessionContext.linkMode!!.expectedPaymentMethodType,
                billingPhone = elementsSessionContext.billingDetails?.phone,
            )

            sharePaymentDetails.encodedPaymentMethod
        } else {
            repository.createPaymentMethod(
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = clientSecret,
                billingDetails = billingDetails,
            )
        }

        val incentiveEligibilitySessionId = elementsSessionContext?.incentiveEligibilitySession?.id

        val eligibleForIncentive = if (incentiveEligibilitySessionId != null) {
            consumerRepository.updateAvailableIncentives(
                sessionId = incentiveEligibilitySessionId,
                paymentDetailsId = paymentDetails.id,
                consumerSessionClientSecret = clientSecret,
            ).map {
                it.data.isNotEmpty()
            }.getOrDefault(false)
        } else {
            false
        }

        return InstantDebitsResult(
            encodedPaymentMethod = paymentMethod,
            bankName = paymentDetails.bankName,
            last4 = paymentDetails.last4,
            eligibleForIncentive = eligibleForIncentive,
        )
    }
}
