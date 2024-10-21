package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.BillingAddress
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
import org.mockito.kotlin.anyOrNull
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
            elementsSessionContext = makeElementsSessionContext(
                linkMode = LinkMode.LinkCardBrand,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(consumerRepository).sharePaymentDetails(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
            expectedPaymentMethodType = "card",
            billingPhone = null,
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
            elementsSessionContext = makeElementsSessionContext(
                linkMode = LinkMode.LinkPaymentMethod,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(repository).createPaymentMethod(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
            billingAddress = null,
            billingEmailAddress = "test@test.com",
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
            elementsSessionContext = makeElementsSessionContext(
                linkMode = null,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(repository).createPaymentMethod(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
            billingAddress = null,
            billingEmailAddress = "test@test.com",
        )
    }

    @Test
    fun `Passes along billing details to createPaymentMethod if available`() = runTest {
        val consumerRepository = makeConsumerSessionRepository()
        val repository = makeRepository()

        val billingDetails = BillingAddress(
            name = "Some name",
            phone = "+15555555555",
            address = BillingAddress.Address(
                city = "San Francisco",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4",
                postalCode = "94111",
                state = "CA",
            ),
        )

        val createInstantDebitResult = RealCreateInstantDebitsResult(
            consumerRepository = consumerRepository,
            repository = repository,
            consumerSessionProvider = { makeCachedConsumerSession() },
            elementsSessionContext = makeElementsSessionContext(
                linkMode = null,
                billingAddress = billingDetails,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(repository).createPaymentMethod(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
            billingAddress = billingDetails,
            billingEmailAddress = "test@test.com",
        )
    }

    @Test
    fun `Passes along billing details to sharePaymentDetails if available`() = runTest {
        val consumerRepository = makeConsumerSessionRepository()
        val repository = makeRepository()

        val billingDetails = BillingAddress(
            name = "Some name",
            phone = "+15555555555",
            address = BillingAddress.Address(
                city = "San Francisco",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4",
                postalCode = "94111",
                state = "CA",
            ),
        )

        val createInstantDebitResult = RealCreateInstantDebitsResult(
            consumerRepository = consumerRepository,
            repository = repository,
            consumerSessionProvider = { makeCachedConsumerSession() },
            elementsSessionContext = makeElementsSessionContext(
                linkMode = LinkMode.LinkCardBrand,
                billingAddress = billingDetails,
            ),
        )

        createInstantDebitResult("bank_account_id_001")

        verify(consumerRepository).sharePaymentDetails(
            paymentDetailsId = "ba_1234",
            consumerSessionClientSecret = "clientSecret",
            expectedPaymentMethodType = "card",
            billingPhone = "+15555555555",
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
            onBlocking { createPaymentDetails(any(), any(), anyOrNull(), any()) } doReturn consumerPaymentDetails
            onBlocking { sharePaymentDetails(any(), any(), any(), anyOrNull()) } doReturn sharePaymentDetails
        }
    }

    private fun makeRepository(): FinancialConnectionsRepository {
        val paymentMethod = PaymentMethod(
            id = "pm_1234",
        )

        return mock<FinancialConnectionsRepository> {
            onBlocking { createPaymentMethod(any(), any(), anyOrNull(), any()) } doReturn paymentMethod
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

    private fun makeElementsSessionContext(
        linkMode: LinkMode?,
        billingAddress: BillingAddress? = null,
    ): ElementsSessionContext {
        return ElementsSessionContext(
            initializationMode = ElementsSessionContext.InitializationMode.PaymentIntent("pi_123"),
            amount = 100L,
            currency = "usd",
            linkMode = linkMode,
            billingAddress = billingAddress,
        )
    }
}
