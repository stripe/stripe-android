package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.ApiKeyFixtures.consumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.VerificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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
        var onConsumerNotFoundCalled = false

        // Act
        lookupConsumerAndStartVerification(
            email = email,
            businessName = "Test Business",
            verificationType = VerificationType.EMAIL,
            onConsumerNotFound = { onConsumerNotFoundCalled = true },
            onLookupError = { fail("onLookupError should not be called") },
            onStartVerification = { fail("onStartVerification should not be called") },
            onVerificationStarted = { fail("onVerificationStarted should not be called") },
            onStartVerificationError = { fail("onStartVerificationError should not be called") }
        )

        // Assert
        assertEquals(true, onConsumerNotFoundCalled)
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

            var onStartVerificationCalled = false
            var onVerificationStartedCalled = false

            // Act
            lookupConsumerAndStartVerification(
                email = email,
                businessName = "Test Business",
                verificationType = expectedVerificationType,
                onConsumerNotFound = { fail("onConsumerNotFound should not be called") },
                onLookupError = { fail("onLookupError should not be called") },
                onStartVerification = { onStartVerificationCalled = true },
                onVerificationStarted = { onVerificationStartedCalled = true },
                onStartVerificationError = { fail("onStartVerificationError should not be called") }
            )

            // Assert
            assertEquals(true, onStartVerificationCalled)
            assertEquals(true, onVerificationStartedCalled)
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
        var onLookupErrorCalled = false

        // Act
        lookupConsumerAndStartVerification(
            email = email,
            businessName = "Test Business",
            verificationType = VerificationType.EMAIL,
            onConsumerNotFound = { fail("onConsumerNotFound should not be called") },
            onLookupError = { onLookupErrorCalled = true },
            onStartVerification = { fail("onStartVerification should not be called") },
            onVerificationStarted = { fail("onVerificationStarted should not be called") },
            onStartVerificationError = { fail("onStartVerificationError should not be called") }
        )

        // Assert
        assertEquals(true, onLookupErrorCalled)
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
        var onStartVerificationErrorCalled = false

        // Act
        lookupConsumerAndStartVerification(
            email = email,
            businessName = "Test Business",
            verificationType = expectedVerificationType,
            onConsumerNotFound = { fail("onConsumerNotFound should not be called") },
            onLookupError = { fail("onLookupError should not be called") },
            onStartVerification = { },
            onVerificationStarted = { fail("onVerificationStarted should not be called") },
            onStartVerificationError = { onStartVerificationErrorCalled = true }
        )

        // Assert
        assertEquals(true, onStartVerificationErrorCalled)
        verify(startVerification).email(
            consumerSessionClientSecret = consumerSession.clientSecret,
            businessName = "Test Business"
        )
    }
}
