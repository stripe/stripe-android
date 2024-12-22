package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSessionSignup
import com.stripe.android.financialconnections.ApiKeyFixtures.verifiedConsumerSession
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionState
import com.stripe.android.model.ConsumerSession.VerificationSession.SessionType
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.model.VerificationType
import com.stripe.android.repository.ConsumersApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
    private val fraudDetectionDataRepository = mock<FraudDetectionDataRepository>()

    private fun buildRepository(
        isInstantDebits: Boolean = false,
        elementsSessionContext: ElementsSessionContext? = null,
    ) = FinancialConnectionsConsumerSessionRepository(
        consumersApiService = consumersApiService,
        provideApiRequestOptions = { apiOptions },
        financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
        consumerSessionRepository = consumerSessionRepository,
        locale = locale,
        logger = logger,
        isLinkWithStripe = { isInstantDebits },
        fraudDetectionDataRepository = fraudDetectionDataRepository,
        elementsSessionContext = elementsSessionContext,
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
                amount = anyOrNull(),
                currency = anyOrNull(),
                incentiveEligibilitySession = anyOrNull(),
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
    fun `Sends relevant fields from ElementsSessionContext in signup call`() = runTest {
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
                amount = eq(1234),
                currency = eq("cad"),
                incentiveEligibilitySession = eq(IncentiveEligibilitySession.PaymentIntent("pi_123")),
                consentAction = anyOrNull(),
                requestSurface = anyOrNull(),
                requestOptions = anyOrNull(),
            )
        ).thenReturn(
            Result.success(consumerSessionSignup)
        )

        val repository = buildRepository(
            elementsSessionContext = ElementsSessionContext(
                amount = 1234,
                currency = "cad",
                linkMode = LinkMode.LinkPaymentMethod,
                billingDetails = null,
                prefillDetails = ElementsSessionContext.PrefillDetails(
                    email = null,
                    phone = null,
                    phoneCountryCode = null,
                ),
                incentiveEligibilitySession = IncentiveEligibilitySession.PaymentIntent("pi_123"),
            )
        )

        repository.signUp(
            email = "email@email.com",
            phoneNumber = "+15555555555",
            country = "US",
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

        val result = repository.postConsumerSession(email, clientSecret)

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

    @Test
    fun `Uses correct request surface in Financial Connections flow`() = runTest {
        val repository = buildRepository(isInstantDebits = false)

        whenever(
            consumersApiService.signUp(
                email = anyOrNull(),
                phoneNumber = anyOrNull(),
                country = anyOrNull(),
                name = anyOrNull(),
                locale = anyOrNull(),
                amount = anyOrNull(),
                currency = anyOrNull(),
                incentiveEligibilitySession = anyOrNull(),
                consentAction = anyOrNull(),
                requestSurface = anyOrNull(),
                requestOptions = anyOrNull(),
            )
        ).thenReturn(Result.success(consumerSessionSignup()))

        repository.signUp(
            email = "someone@something.ca",
            phoneNumber = "+15555555555",
            country = "US",
        )

        verify(consumersApiService).signUp(
            email = anyOrNull(),
            phoneNumber = anyOrNull(),
            country = anyOrNull(),
            name = anyOrNull(),
            locale = anyOrNull(),
            amount = anyOrNull(),
            currency = anyOrNull(),
            incentiveEligibilitySession = anyOrNull(),
            requestSurface = eq("android_connections"),
            consentAction = anyOrNull(),
            requestOptions = anyOrNull(),
        )
    }

    @Test
    fun `Uses correct request surface in Instant Debits flow`() = runTest {
        val repository = buildRepository(isInstantDebits = true)

        whenever(
            consumersApiService.signUp(
                email = anyOrNull(),
                phoneNumber = anyOrNull(),
                country = anyOrNull(),
                name = anyOrNull(),
                locale = anyOrNull(),
                amount = anyOrNull(),
                currency = anyOrNull(),
                incentiveEligibilitySession = anyOrNull(),
                consentAction = anyOrNull(),
                requestSurface = anyOrNull(),
                requestOptions = anyOrNull(),
            )
        ).thenReturn(Result.success(consumerSessionSignup()))

        repository.signUp(
            email = "someone@something.ca",
            phoneNumber = "+15555555555",
            country = "US",
        )

        verify(consumersApiService).signUp(
            email = anyOrNull(),
            phoneNumber = anyOrNull(),
            country = anyOrNull(),
            name = anyOrNull(),
            locale = anyOrNull(),
            amount = anyOrNull(),
            currency = anyOrNull(),
            incentiveEligibilitySession = anyOrNull(),
            requestSurface = eq("android_instant_debits"),
            consentAction = anyOrNull(),
            requestOptions = anyOrNull(),
        )
    }

    @Test
    fun `Sends fraud detection data when sharing PaymentDetails`() = runTest {
        val consumerSessionClientSecret = "clientSecret"
        val repository = buildRepository()

        val fraudParams = FraudDetectionData(
            guid = "guid_1234",
            muid = "muid_1234",
            sid = "sid_1234",
            timestamp = 1234567890L,
        )

        whenever(fraudDetectionDataRepository.getCached()).thenReturn(fraudParams)

        whenever(
            consumersApiService.sharePaymentDetails(
                consumerSessionClientSecret = anyOrNull(),
                paymentDetailsId = anyOrNull(),
                expectedPaymentMethodType = anyOrNull(),
                billingPhone = anyOrNull(),
                requestSurface = anyOrNull(),
                requestOptions = anyOrNull(),
                extraParams = eq(fraudParams.params),
            )
        ).thenReturn(
            Result.success(
                SharePaymentDetails(
                    paymentMethodId = "pm_123",
                    encodedPaymentMethod = "{\"id\": \"pm_123\"}",
                )
            )
        )

        repository.sharePaymentDetails(
            consumerSessionClientSecret = consumerSessionClientSecret,
            paymentDetailsId = "pd_123",
            expectedPaymentMethodType = "card",
            billingPhone = null,
        )

        verify(fraudDetectionDataRepository, never()).getLatest()
    }
}
