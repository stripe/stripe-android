package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.TestFactory
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class DefaultLinkAuthTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    @Test
    fun `config with attestation enabled successfully signs in`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.signUp(
            email = TestFactory.CUSTOMER_EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        val accountManagerCall = linkAccountManager.awaitMobileSignUpCall()
        integrityRequestManager.awaitRequestTokenCall()

        assertThat(accountManagerCall.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.country).isEqualTo(TestFactory.COUNTRY)
        assertThat(accountManagerCall.phone).isEqualTo(TestFactory.CUSTOMER_PHONE)
        assertThat(accountManagerCall.consentAction).isEqualTo(SignUpConsentAction.Implied)
        assertThat(accountManagerCall.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(accountManagerCall.appId).isEqualTo(TestFactory.APP_ID)

        assertThat(result).isEqualTo(LinkAuthResult.Success(TestFactory.LINK_ACCOUNT))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `sign up attempt with attestation failure returns AttestationFailed`() = runTest {
        val error = APIException(
            stripeError = StripeError(
                code = "link_failed_to_attest_request"
            )
        )
        val errorReporter = FakeErrorReporter()
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            errorReporter = errorReporter
        )

        val result = linkAuth.signUp(
            email = TestFactory.CUSTOMER_EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        integrityRequestManager.awaitRequestTokenCall()

        val errorReport = errorReporter.awaitCall()
        assertThat(errorReport.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_ATTEST_REQUEST)
        assertThat(errorReport.additionalNonPiiParams)
            .containsExactly("operation", "signup")

        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `sign up attempt with token fetch failure returns AttestationFailed`() = runTest {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.INTERNAL_ERROR,
            message = "oops"
        )
        val errorReporter = FakeErrorReporter()
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            errorReporter = errorReporter
        )

        val result = linkAuth.signUp(
            email = TestFactory.CUSTOMER_EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        integrityRequestManager.awaitRequestTokenCall()

        val errorReport = errorReporter.awaitCall()
        assertThat(errorReport.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_GET_INTEGRITY_TOKEN)
        assertThat(errorReport.additionalNonPiiParams)
            .containsExactly("operation", "signup")
        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `sign in attempt with generic failure returns Error`() = runTest {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.signUp(
            email = TestFactory.CUSTOMER_EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        integrityRequestManager.awaitRequestTokenCall()

        assertThat(result).isEqualTo(LinkAuthResult.Error(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `config with attestation disabled successfully signs in`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        val linkAuth = linkAuth(
            useAttestationEndpoints = false,
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.signUp(
            email = TestFactory.CUSTOMER_EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        val accountManagerCall = linkAccountManager.awaitSignUpCall()

        assertThat(accountManagerCall.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.country).isEqualTo(TestFactory.COUNTRY)
        assertThat(accountManagerCall.phone).isEqualTo(TestFactory.CUSTOMER_PHONE)
        assertThat(accountManagerCall.consentAction).isEqualTo(SignUpConsentAction.Implied)

        assertThat(result).isEqualTo(LinkAuthResult.Success(TestFactory.LINK_ACCOUNT))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `config with attestation disabled returns error on sign up failure`() = runTest {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        linkAccountManager.signUpResult = Result.failure(error)

        val linkAuth = linkAuth(
            useAttestationEndpoints = false,
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.signUp(
            email = TestFactory.CUSTOMER_EMAIL,
            phoneNumber = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        val accountManagerCall = linkAccountManager.awaitSignUpCall()

        assertThat(accountManagerCall.name).isEqualTo(TestFactory.CUSTOMER_NAME)
        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.country).isEqualTo(TestFactory.COUNTRY)
        assertThat(accountManagerCall.phone).isEqualTo(TestFactory.CUSTOMER_PHONE)
        assertThat(accountManagerCall.consentAction).isEqualTo(SignUpConsentAction.Implied)

        assertThat(result).isEqualTo(LinkAuthResult.Error(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `config with attestation enabled performs lookup successfully`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = true
        )

        val accountManagerCall = linkAccountManager.awaitMobileLookupCall()
        integrityRequestManager.awaitRequestTokenCall()

        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.emailSource).isEqualTo(TestFactory.EMAIL_SOURCE)
        assertThat(accountManagerCall.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(accountManagerCall.appId).isEqualTo(TestFactory.APP_ID)
        assertThat(accountManagerCall.startSession).isTrue()

        assertThat(result).isEqualTo(LinkAuthResult.Success(TestFactory.LINK_ACCOUNT))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `lookup attempt with attestation failure returns AttestationFailed`() = runTest {
        val error = APIException(
            stripeError = StripeError(
                code = "link_failed_to_attest_request"
            )
        )
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()
        val errorReporter = FakeErrorReporter()

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            errorReporter = errorReporter
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = false
        )

        integrityRequestManager.awaitRequestTokenCall()

        val errorReport = errorReporter.awaitCall()
        assertThat(errorReport.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_ATTEST_REQUEST)
        assertThat(errorReport.additionalNonPiiParams)
            .containsExactly("operation", "lookup")

        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `lookup attempt with token fetch failure returns AttestationFailed`() = runTest {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.INTERNAL_ERROR,
            message = "oops"
        )
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()
        val errorReporter = FakeErrorReporter()

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            errorReporter = errorReporter
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = false
        )

        integrityRequestManager.awaitRequestTokenCall()

        val errorReport = errorReporter.awaitCall()
        assertThat(errorReport.errorEvent)
            .isEqualTo(ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_GET_INTEGRITY_TOKEN)
        assertThat(errorReport.additionalNonPiiParams)
            .containsExactly("operation", "lookup")
        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `lookup attempt with generic failure returns Error`() = runTest {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = false
        )

        integrityRequestManager.awaitRequestTokenCall()

        assertThat(result).isEqualTo(LinkAuthResult.Error(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `config with attestation disabled performs lookup successfully`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        linkAccountManager.lookupConsumerResult = Result.success(TestFactory.LINK_ACCOUNT)

        val linkAuth = linkAuth(
            useAttestationEndpoints = false,
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = false
        )

        val accountManagerCall = linkAccountManager.awaitLookupCall()

        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.startSession).isFalse()

        assertThat(result).isEqualTo(LinkAuthResult.Success(TestFactory.LINK_ACCOUNT))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `config with attestation disabled returns error on lookup failure`() = runTest {
        val error = Throwable("oops")
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        linkAccountManager.lookupConsumerResult = Result.failure(error)

        val linkAuth = linkAuth(
            useAttestationEndpoints = false,
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = true
        )

        val accountManagerCall = linkAccountManager.awaitLookupCall()

        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.startSession).isTrue()

        assertThat(result).isEqualTo(LinkAuthResult.Error(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `null link account yields NoLinkAccountFound result`() = runTest {
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        linkAccountManager.mobileLookupConsumerResult = Result.success(null)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = false
        )

        val accountManagerCall = linkAccountManager.awaitMobileLookupCall()
        integrityRequestManager.awaitRequestTokenCall()

        assertThat(accountManagerCall.startSession).isFalse()
        assertThat(result).isEqualTo(LinkAuthResult.NoLinkAccountFound)

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `lookup attempt with account error returns AccountError`() = runTest {
        val error = APIException(
            stripeError = StripeError(
                code = "link_consumer_details_not_available"
            )
        )
        val linkAccountManager = FakeLinkAccountManager()
        val integrityRequestManager = FakeIntegrityRequestManager()

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE,
            startSession = false
        )

        integrityRequestManager.awaitRequestTokenCall()

        assertThat(result).isEqualTo(LinkAuthResult.AccountError(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    private fun linkAuth(
        useAttestationEndpoints: Boolean = true,
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager(),
        errorReporter: ErrorReporter = FakeErrorReporter()
    ): DefaultLinkAuth {
        return DefaultLinkAuth(
            linkGate = FakeLinkGate().apply {
                setUseAttestationEndpoints(useAttestationEndpoints)
            },
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            errorReporter = errorReporter,
            applicationId = TestFactory.APP_ID
        )
    }
}
