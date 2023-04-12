package com.stripe.android.financialconnections.repository

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
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
    private val apiOptions: ApiRequest.Options = mock()
    private val logger: Logger = mock()

    private fun buildRepository() =
        FinancialConnectionsConsumerSessionRepository(
            consumersApiService = consumersApiService,
            apiOptions = apiOptions,
            locale = null,
            logger = logger
        )

    @Test
    fun testLookupConsumerSession() = runTest {
        val email = "test@example.com"
        val consumerSession = consumerSession()
        val consumerSessionLookup = ConsumerSessionLookup(
            consumerSession = consumerSession,
            errorMessage = null,
            exists = true,
        )
        val repository = buildRepository()

        whenever(
            consumersApiService.lookupConsumerSession(
                email = eq(email),
                authSessionCookie = anyOrNull(),
                requestSurface = eq("android_connections"),
                requestOptions = eq(apiOptions)
            )
        ).thenReturn(consumerSessionLookup)

        // ensures there's no cached consumer session
        assertNull(repository.getCachedConsumerSession())

        val result = repository.lookupConsumerSession(email)

        assertEquals(consumerSessionLookup, result)

        verify(consumersApiService).lookupConsumerSession(
            email = email,
            authSessionCookie = null,
            requestSurface = "android_connections",
            requestOptions = apiOptions
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
        val locale = Locale.getDefault()

        whenever(
            consumersApiService.startConsumerVerification(
                consumerSessionClientSecret = eq(consumerSessionClientSecret),
                locale = eq(locale),
                authSessionCookie = anyOrNull(),
                requestSurface = eq("android_connections"),
                type = eq(type),
                customEmailType = eq(customEmailType),
                requestOptions = eq(apiOptions)
            )
        ).thenReturn(consumerSession)

        // ensures there's no cached consumer session
        assertNull(repository.getCachedConsumerSession())

        val result = repository
            .startConsumerVerification(consumerSessionClientSecret, type, customEmailType)

        assertEquals(consumerSession, result)
        verify(consumersApiService).startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            locale = locale,
            authSessionCookie = null,
            requestSurface = "android_connections",
            type = type,
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
