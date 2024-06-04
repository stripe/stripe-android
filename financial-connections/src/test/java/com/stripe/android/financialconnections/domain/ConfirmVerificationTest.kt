package com.stripe.android.financialconnections.domain

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
internal class ConfirmVerificationTest {

    private val consumerSessionRepository: FinancialConnectionsConsumerSessionRepository = mock()
    private lateinit var confirmVerification: ConfirmVerification

    @Before
    fun setUp() {
        confirmVerification = ConfirmVerification(consumerSessionRepository)
    }

    @Test
    fun `test SMS verification success`() = runTest {
        val clientSecret = "client_secret"
        val verificationCode = "123456"
        val consumerSession: ConsumerSession = mock()

        whenever(
            consumerSessionRepository.confirmConsumerVerification(
                consumerSessionClientSecret = clientSecret,
                verificationCode = verificationCode,
                type = VerificationType.SMS
            )
        ).thenReturn(consumerSession)

        val result = confirmVerification.sms(clientSecret, verificationCode)

        verify(consumerSessionRepository).confirmConsumerVerification(
            clientSecret,
            verificationCode,
            VerificationType.SMS
        )
        assertEquals(consumerSession, result)
    }

    @Test
    fun `test Email verification success`() = runTest {
        val clientSecret = "client_secret"
        val verificationCode = "123456"
        val consumerSession: ConsumerSession = mock()

        whenever(
            consumerSessionRepository.confirmConsumerVerification(
                consumerSessionClientSecret = clientSecret,
                verificationCode = verificationCode,
                type = VerificationType.EMAIL
            )
        ).thenReturn(consumerSession)

        val result = confirmVerification.email(clientSecret, verificationCode)

        verify(consumerSessionRepository).confirmConsumerVerification(
            clientSecret,
            verificationCode,
            VerificationType.EMAIL
        )
        assertEquals(consumerSession, result)
    }

    @Test
    fun `test SMS verification invalid code`() = runTest {
        val clientSecret = "client_secret"
        val verificationCode = "123456"
        val errorMessage = "consumer_verification_code_invalid"

        whenever(
            consumerSessionRepository.confirmConsumerVerification(
                consumerSessionClientSecret = clientSecret,
                verificationCode = verificationCode,
                type = VerificationType.SMS
            )
        ).thenAnswer {
            throw InvalidRequestException(
                StripeError(
                    code = errorMessage,
                    message = errorMessage
                )
            )
        }

        val exception: OTPError =
            runCatching { confirmVerification.sms(clientSecret, verificationCode) }
                .exceptionOrNull() as OTPError

        assertEquals(errorMessage, exception.message)
        assertEquals(OTPError.Type.CODE_INVALID, exception.type)
    }

    @Test
    fun `test Email verification invalid code`() = runTest {
        val clientSecret = "client_secret"
        val verificationCode = "123456"
        val errorMessage = "consumer_verification_code_invalid"

        whenever(
            consumerSessionRepository.confirmConsumerVerification(
                consumerSessionClientSecret = clientSecret,
                verificationCode = verificationCode,
                type = VerificationType.EMAIL
            )
        ).thenAnswer {
            throw InvalidRequestException(
                StripeError(
                    code = errorMessage,
                    message = errorMessage
                )
            )
        }

        val exception: OTPError =
            runCatching { confirmVerification.email(clientSecret, verificationCode) }
                .exceptionOrNull() as OTPError

        assertEquals(errorMessage, exception.message)
        assertEquals(OTPError.Type.CODE_INVALID, exception.type)
    }

    @Test
    fun `test SMS verification expired`() = runTest {
        val clientSecret = "client_secret"
        val verificationCode = "123456"
        val errorMessage = "consumer_verification_expired"

        whenever(
            consumerSessionRepository.confirmConsumerVerification(
                consumerSessionClientSecret = clientSecret,
                verificationCode = verificationCode,
                type = VerificationType.SMS
            )
        ).thenAnswer {
            throw InvalidRequestException(
                StripeError(
                    code = errorMessage,
                    message = errorMessage
                )
            )
        }

        val exception: OTPError =
            runCatching { confirmVerification.sms(clientSecret, verificationCode) }
                .exceptionOrNull() as OTPError

        assertEquals(errorMessage, exception.message)
        assertEquals(OTPError.Type.SMS_CODE_EXPIRED, exception.type)
    }

    @Test
    fun `test Email verification expired`() = runTest {
        val clientSecret = "client_secret"
        val verificationCode = "123456"
        val errorMessage = "consumer_verification_expired"

        whenever(
            consumerSessionRepository.confirmConsumerVerification(
                consumerSessionClientSecret = clientSecret,
                verificationCode = verificationCode,
                type = VerificationType.EMAIL
            )
        ).thenAnswer {
            throw InvalidRequestException(
                StripeError(
                    code = errorMessage,
                    message = errorMessage
                )
            )
        }

        val exception: OTPError =
            runCatching { confirmVerification.email(clientSecret, verificationCode) }
                .exceptionOrNull() as OTPError

        assertEquals(errorMessage, exception.message)
        assertEquals(OTPError.Type.EMAIL_CODE_EXPIRED, exception.type)
    }
}
