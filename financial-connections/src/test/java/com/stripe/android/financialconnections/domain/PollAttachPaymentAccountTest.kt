package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures.institution
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.exception.AccountNumberRetrievalError
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod.DESCRIPTOR_CODE
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

internal class PollAttachPaymentAccountTest {

    private val consumerSession = cachedConsumerSession()
    private val repository = mock(FinancialConnectionsAccountsRepository::class.java)
    private val consumerSessionRepository = ConsumerSessionProvider { consumerSession }
    private val attachedPaymentAccountRepository = mock(AttachedPaymentAccountRepository::class.java)
    private val configuration = FinancialConnectionsSheetConfiguration(
        financialConnectionsSessionClientSecret = "client_secret",
        publishableKey = "publishable_key"
    )

    private val pollAttachPaymentAccount = PollAttachPaymentAccount(
        repository,
        consumerSessionRepository,
        attachedPaymentAccountRepository,
        configuration
    )

    @Test
    fun `Successfully attaches payment account passing consumer session secret`() = runTest {
        val sync = syncResponse()

        val params = PaymentAccountParams.BankAccount(
            accountNumber = "acct_123",
            routingNumber = "110000000",
        )
        val paymentAccount = LinkAccountSessionPaymentAccount(
            id = "acct_123",
            microdepositVerificationMethod = DESCRIPTOR_CODE,
        )

        whenever(
            repository.postAttachPaymentAccountToLinkAccountSession(
                clientSecret = anyString(),
                paymentAccount = eq(params),
                consumerSessionClientSecret = eq(consumerSession.clientSecret)
            )
        )
            .thenReturn(paymentAccount)

        val result = pollAttachPaymentAccount(sync, null, params)

        assertThat(result).isEqualTo(paymentAccount)
        verify(attachedPaymentAccountRepository).set(params)
    }

    @Test
    fun `Handles manual entry error correctly`() = runTest {
        val sync = syncResponse()
        val params = PaymentAccountParams.BankAccount(
            accountNumber = "acct_123",
            routingNumber = "110000000",
        )
        val institution = institution()
        val exception = accountNUmberRetrievalError()

        whenever(
            repository.postAttachPaymentAccountToLinkAccountSession(
                clientSecret = anyOrNull(),
                paymentAccount = anyOrNull(),
                consumerSessionClientSecret = anyOrNull()
            )
        ).thenAnswer {
            throw exception
        }

        try {
            pollAttachPaymentAccount(sync = sync, activeInstitution = institution, params = params)
        } catch (e: AccountNumberRetrievalError) {
            assertThat(e.institution).isEqualTo(institution)
        }
    }

    private fun accountNUmberRetrievalError() = APIException(
        message = "Error",
        statusCode = 400,
        requestId = "req_123",
        stripeError = StripeError(
            code = "account_number_retrieval_failed",
            message = "Account number retrieval failed",
            extraFields = mapOf("reason" to "account_number_retrieval_failed")
        )
    )

    private fun cachedConsumerSession() = CachedConsumerSession(
        clientSecret = "clientSecret",
        emailAddress = "test@test.com",
        isVerified = true,
        phoneNumber = "+1********12",
        publishableKey = null
    )
}
