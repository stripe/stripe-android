package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.model.PaymentMethod
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkMode
import com.stripe.android.model.SharePaymentDetails
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class RealCreateInstantDebitsResultTest {

    @Test
    fun `Calls correct endpoint for Link card brand`() = runTest {
        val consumerRepository = makeConsumerSessionRepository()
        val repository = makeRepository()

        val createInstantDebitResult = RealCreateInstantDebitsResult(
            consumerRepository = consumerRepository,
            repository = repository,
            consumerSessionProvider = { makeCachedConsumerSession() },
            elementsSessionContext = ElementsSessionContext(
                linkMode = LinkMode.LinkCardBrand,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(consumerRepository).sharePaymentDetails(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
            expectedPaymentMethodType = "card",
        )

        verifyNoInteractions(repository)
    }

    @Test
    fun `Calls correct endpoint for Instant Debits`() = runTest {
        val consumerRepository = makeConsumerSessionRepository()
        val repository = makeRepository()

        val createInstantDebitResult = RealCreateInstantDebitsResult(
            consumerRepository = consumerRepository,
            repository = repository,
            consumerSessionProvider = { makeCachedConsumerSession() },
            elementsSessionContext = ElementsSessionContext(
                linkMode = LinkMode.LinkPaymentMethod,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(repository).createPaymentMethod(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
        )
    }

    @Test
    fun `Calls correct endpoint if no Link mode available`() = runTest {
        val consumerRepository = makeConsumerSessionRepository()
        val repository = makeRepository()

        val createInstantDebitResult = RealCreateInstantDebitsResult(
            consumerRepository = consumerRepository,
            repository = repository,
            consumerSessionProvider = { makeCachedConsumerSession() },
            elementsSessionContext = ElementsSessionContext(
                linkMode = null,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(repository).createPaymentMethod(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
        )
    }

    private fun makeConsumerSessionRepository(): FinancialConnectionsConsumerSessionRepository {
        val consumerPaymentDetails = ConsumerPaymentDetails(
            paymentDetails = listOf(
                ConsumerPaymentDetails.BankAccount(
                    id = "ba_1234",
                    bankName = "Stripe Bank",
                    last4 = "4242",
                )
            )
        )

        val sharePaymentDetails = SharePaymentDetails(
            paymentMethodId = "pm_1234",
        )

        return mock<FinancialConnectionsConsumerSessionRepository> {
            onBlocking { createPaymentDetails(any(), any()) } doReturn consumerPaymentDetails
            onBlocking { sharePaymentDetails(any(), any(), any()) } doReturn sharePaymentDetails
        }
    }

    private fun makeRepository(): FinancialConnectionsRepository {
        val paymentMethod = PaymentMethod(
            id = "pm_1234",
        )

        return mock<FinancialConnectionsRepository> {
            onBlocking { createPaymentMethod(any(), any()) } doReturn paymentMethod
        }
    }

    private fun makeCachedConsumerSession(): CachedConsumerSession {
        return CachedConsumerSession(
            clientSecret = "clientSecret",
            emailAddress = "test@test.com",
            phoneNumber = "(***) *** **12",
            isVerified = true,
            publishableKey = "pk_123",
        )
    }
}
