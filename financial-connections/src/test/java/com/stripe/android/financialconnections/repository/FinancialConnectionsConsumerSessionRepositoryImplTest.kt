package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class FinancialConnectionsConsumerSessionRepositoryImplTest {

    private val consumersApiService: ConsumersApiService = mock()
    private val financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService =
        mock()
    private val apiOptions: ApiRequest.Options = mock()
    private val logger: Logger = mock()
    private val locale: Locale = Locale.getDefault()

    private fun buildRepository() =
        FinancialConnectionsConsumerSessionRepository(
            consumersApiService = consumersApiService,
            financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
            apiOptions = apiOptions,
            locale = locale,
            logger = logger
        )

    @Test
    fun testLookupConsumerSession() = runTest {
        val email = "test@example.com"
        val clientSecret = "client_secret"
        val consumerSession = consumerSession()
        val consumerSessionLookup = ConsumerSessionLookup(
            consumerSession = consumerSession,
            errorMessage = null,
            exists = true,
        )
        val repository = buildRepository()

        whenever(
            financialConnectionsConsumersApiService.postConsumerSession(
                email = eq(email),
                clientSecret = eq(clientSecret),
                requestSurface = eq("android_connections"),
            )
        ).thenReturn(consumerSessionLookup)

        // ensures there's no cached consumer session
        assertNull(repository.getCachedConsumerSession())

        val result = repository.lookupConsumerSession(email, clientSecret)

        assertEquals(consumerSessionLookup, result)

        verify(financialConnectionsConsumersApiService).postConsumerSession(
            email = email,
            clientSecret = clientSecret,
            requestSurface = "android_connections",
        )

        // ensures there's a cached consumer session after the lookup call.
        assertEquals(repository.getCachedConsumerSession(), consumerSession)
    }

    @Test
    fun testStartConsumerVerification() = runTest {
        val consumerSessionClientSecret = "clientSecret"
        val type = VerificationType.EMAIL
        val customEmailType = CustomEmailType.NETWORKED_CONNECTIONS_OTP_EMAIL
        val consumerSession = consumerSession()
        val repository = buildRepository()
        val connectionsMerchantName = "my_merchant"

        whenever(
            consumersApiService.startConsumerVerification(
                consumerSessionClientSecret = eq(consumerSessionClientSecret),
                locale = eq(locale),
                authSessionCookie = anyOrNull(),
                requestSurface = eq("android_connections"),
                type = eq(type),
                connectionsMerchantName = anyOrNull(),
                customEmailType = anyOrNull(),
                requestOptions = eq(apiOptions)
            )
        ).thenReturn(consumerSession)

        // ensures there's no cached consumer session
        assertNull(repository.getCachedConsumerSession())

        val result: ConsumerSession = repository.startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            connectionsMerchantName = connectionsMerchantName,
            type = type,
            customEmailType = customEmailType
        )

        assertEquals(consumerSession, result)
        verify(consumersApiService).startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            locale = locale,
            authSessionCookie = null,
            requestSurface = "android_connections",
            type = type,
            connectionsMerchantName = connectionsMerchantName,
            customEmailType = customEmailType,
            requestOptions = apiOptions
        )

        // ensures there's a cached consumer session after the lookup call.
        assertEquals(repository.getCachedConsumerSession(), consumerSession)
    }

    @Test
    fun testConfirmConsumerVerification() = runTest {
        val consumerSessionClientSecret = "clientSecret"
        val verificationCode = "123456"
        val type = VerificationType.EMAIL
        val consumerSession = consumerSession()
        val repository = buildRepository()

        whenever(
            consumersApiService.confirmConsumerVerification(
                consumerSessionClientSecret = eq(consumerSessionClientSecret),
                verificationCode = eq(verificationCode),
                authSessionCookie = anyOrNull(),
                requestSurface = eq("android_connections"),
                type = eq(type),
                requestOptions = eq(apiOptions)
            )
        ).thenReturn(consumerSession)

        // ensures there's no cached consumer session
        assertNull(repository.getCachedConsumerSession())

        val result = repository.confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode,
            type = type
        )
        assertEquals(consumerSession, result)
        verify(consumersApiService).confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode,
            authSessionCookie = null,
            "android_connections",
            type,
            apiOptions
        )

        // ensures there's a cached consumer session after the lookup call.
        assertEquals(repository.getCachedConsumerSession(), consumerSession)
    }
}
