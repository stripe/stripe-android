package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.financialconnections.domain.LookupConsumerAndStartVerification.Result
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.VerificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LookupConsumerAndStartVerificationTest {

    private val lookupAccount = mock<LookupAccount>()
    private val startVerification = mock<StartVerification>()

    private val lookupConsumerAndStartVerification = LookupConsumerAndStartVerification(
        lookupAccount = lookupAccount,
        startVerification = startVerification,
    )

    @Test
    fun `invoke - onConsumerNotFound when lookupAccount returns non existent`() = runTest {
        // Arrange
        val email = "test@test.com"
        val lookup = ConsumerSessionLookup(
            exists = false,
            errorMessage = null,
            consumerSession = null
        )
        whenever(lookupAccount.invoke(email)).thenReturn(lookup)

        // Act
        val verificationResult = lookupConsumerAndStartVerification(
            email = email,
            businessName = "Test Business",
            verificationType = VerificationType.EMAIL,
        )

        // Assert
        assertThat(verificationResult).isInstanceOf(Result.ConsumerNotFound::class.java)
        verifyNoInteractions(startVerification)
    }

    @Test
    fun `invoke - onStartVerification and onVerificationStarted when lookupAccount succeeds`() =
        runTest {
            // Arrange
            val email = "test@test.com"
            val consumerSession = consumerSession()
            val lookup = ConsumerSessionLookup(
                exists = true,
                errorMessage = null,
                consumerSession = consumerSession
            )
            val expectedVerificationType = VerificationType.EMAIL

            whenever(lookupAccount(email)).thenReturn(lookup)
            whenever(
                startVerification.email(
                    consumerSessionClientSecret = consumerSession.clientSecret,
                    businessName = "Test Business"
                )
            ).thenReturn(
                consumerSession
            )

            // Act
            val verificationResult = lookupConsumerAndStartVerification(
                email = email,
                businessName = "Test Business",
                verificationType = expectedVerificationType,
            )

            assertThat(verificationResult).isInstanceOf(Result.Success::class.java)

            // Assert
            verify(startVerification).email(
                consumerSessionClientSecret = consumerSession.clientSecret,
                businessName = "Test Business"
            )
        }

    @Test
    fun `invoke - onLookupError when lookupAccount throws an exception`() = runTest {
        // Arrange
        val email = "test@test.com"
        val expectedException = RuntimeException("Mock lookupAccount exception")
        whenever(lookupAccount(email)).thenThrow(expectedException)

        // Act
        val verificationResult = lookupConsumerAndStartVerification(
            email = email,
            businessName = "Test Business",
            verificationType = VerificationType.EMAIL,
        )

        // Assert
        assertThat(verificationResult).isInstanceOf(Result.LookupError::class.java)
        verifyNoInteractions(startVerification)
    }

    @Test
    fun `invoke - onStartVerificationError when startVerification throws an exception`() = runTest {
        val email = "test@test.com"
        val consumerSession = consumerSession()
        val lookup = ConsumerSessionLookup(
            exists = true,
            errorMessage = null,
            consumerSession = consumerSession
        )
        val expectedVerificationType = VerificationType.EMAIL
        val expectedException = RuntimeException("Mock startVerification exception")

        whenever(lookupAccount(email)).thenReturn(lookup)
        whenever(
            startVerification.email(
                consumerSessionClientSecret = consumerSession.clientSecret,
                businessName = "Test Business"
            )
        ).thenThrow(expectedException)

        // Act
        val verificationResult = lookupConsumerAndStartVerification(
            email = email,
            businessName = "Test Business",
            verificationType = expectedVerificationType,
        )

        // Assert
        assertThat(verificationResult).isInstanceOf(Result.VerificationError::class.java)
        verify(startVerification).email(
            consumerSessionClientSecret = consumerSession.clientSecret,
            businessName = "Test Business"
        )
    }
}
