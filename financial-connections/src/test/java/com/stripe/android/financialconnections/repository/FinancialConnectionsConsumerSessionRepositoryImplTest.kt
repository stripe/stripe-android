package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.verifiedConsumerSession
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionState
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionType
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Locale

@ExperimentalCoroutinesApi
class FinancialConnectionsConsumerSessionRepositoryImplTest {

    private val consumersApiService: ConsumersApiService = mock()
    private val financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService =
        mock()
    private val apiOptions: ApiRequest.Options = ApiRequest.Options(
        apiKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
    )
    private val logger: Logger = mock()
    private val locale: Locale = Locale.getDefault()
    private val consumerSessionRepository = RealConsumerSessionRepository(SavedStateHandle())

    private fun buildRepository() =
        FinancialConnectionsConsumerSessionRepository(
            consumersApiService = consumersApiService,
            provideApiRequestOptions = { apiOptions },
            financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
            consumerSessionRepository = consumerSessionRepository,
            locale = locale,
            logger = logger
        )

    @Test
    fun testSignUp() = runTest {
        val consumerSession = consumerSession().copy(
            verificationSessions = listOf(
                ConsumerSession.VerificationSession(
                    type = SessionType.SignUp,
                    state = SessionState.Started,
                )
            )
        )

        val consumerSessionSignup = ConsumerSessionSignup(
            consumerSession = consumerSession,
            publishableKey = "pk_123",
        )

        whenever(
            consumersApiService.signUp(
                email = anyOrNull(),
                phoneNumber = anyOrNull(),
                country = anyOrNull(),
                name = anyOrNull(),
                locale = anyOrNull(),
                consentAction = anyOrNull(),
                requestSurface = anyOrNull(),
                requestOptions = anyOrNull(),
            )
        ).thenReturn(Result.success(consumerSessionSignup))

        val repository = buildRepository()

        // ensures there's no cached consumer session
        assertThat(repository.getCachedConsumerSession()).isNull()

        val result = repository.signUp(
            email = "email@email.com",
            phoneNumber = "+15555555555",
            country = "US",
        )

        assertThat(result).isEqualTo(consumerSessionSignup)

        // ensures there's a cached consumer session after the signup call.
        assertThat(repository.getCachedConsumerSession()).isEqualTo(
            CachedConsumerSession(
                clientSecret = "clientSecret",
                emailAddress = "test@test.com",
                phoneNumber = "(•••) ••• ••12",
                publishableKey = "pk_123",
                isVerified = true,
            )
        )
    }

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
        assertThat(repository.getCachedConsumerSession()).isNull()

        val result = repository.lookupConsumerSession(email, clientSecret)

        assertThat(result).isEqualTo(consumerSessionLookup)

        verify(financialConnectionsConsumersApiService).postConsumerSession(
            email = email,
            clientSecret = clientSecret,
            requestSurface = "android_connections",
        )

        // ensures there's a cached consumer session after the lookup call.
        assertThat(repository.getCachedConsumerSession()).isEqualTo(
            CachedConsumerSession(
                clientSecret = "clientSecret",
                emailAddress = "test@test.com",
                phoneNumber = "(•••) ••• ••12",
                publishableKey = null,
                isVerified = false,
            )
        )
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
                requestSurface = eq("android_connections"),
                type = eq(type),
                connectionsMerchantName = anyOrNull(),
                customEmailType = anyOrNull(),
                requestOptions = eq(apiOptions)
            )
        ).thenReturn(consumerSession)

        // ensures there's no cached consumer session
        assertThat(repository.getCachedConsumerSession()).isNull()

        val result: ConsumerSession = repository.startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            connectionsMerchantName = connectionsMerchantName,
            type = type,
            customEmailType = customEmailType
        )

        assertThat(result).isEqualTo(consumerSession)

        verify(consumersApiService).startConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            locale = locale,
            requestSurface = "android_connections",
            type = type,
            connectionsMerchantName = connectionsMerchantName,
            customEmailType = customEmailType,
            requestOptions = apiOptions
        )

        // ensures there's a cached consumer session after the start-verification call.
        assertThat(repository.getCachedConsumerSession()).isEqualTo(
            CachedConsumerSession(
                clientSecret = "clientSecret",
                emailAddress = "test@test.com",
                phoneNumber = "(•••) ••• ••12",
                publishableKey = null,
                isVerified = false,
            )
        )
    }

    @Test
    fun testConfirmConsumerVerification() = runTest {
        val consumerSessionClientSecret = "clientSecret"
        val verificationCode = "123456"
        val type = VerificationType.EMAIL
        val consumerSession = verifiedConsumerSession()
        val repository = buildRepository()

        whenever(
            consumersApiService.confirmConsumerVerification(
                consumerSessionClientSecret = eq(consumerSessionClientSecret),
                verificationCode = eq(verificationCode),
                requestSurface = eq("android_connections"),
                type = eq(type),
                requestOptions = eq(apiOptions)
            )
        ).thenReturn(consumerSession)

        // ensures there's no cached consumer session
        assertThat(repository.getCachedConsumerSession()).isNull()

        val result = repository.confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode,
            type = type
        )

        assertThat(result).isEqualTo(consumerSession)

        verify(consumersApiService).confirmConsumerVerification(
            consumerSessionClientSecret = consumerSessionClientSecret,
            verificationCode = verificationCode,
            "android_connections",
            type,
            apiOptions
        )

        // ensures there's a cached consumer session after the confirm-verification call.
        assertThat(repository.getCachedConsumerSession()).isEqualTo(
            CachedConsumerSession(
                clientSecret = "clientSecret",
                emailAddress = "test@test.com",
                phoneNumber = "(•••) ••• ••12",
                publishableKey = null,
                isVerified = true,
            )
        )
    }
}
