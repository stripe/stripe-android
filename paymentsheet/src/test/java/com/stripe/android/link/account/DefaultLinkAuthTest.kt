package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.TestFactory
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.EmailSource
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.attestation.AttestationError
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultLinkAuthTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    private val fakeLinkGate = FakeLinkGate()
    private val fakeLinkRepository = FakeLinkRepository()
    private val fakeIntegrityRequestManager = FakeIntegrityRequestManager()
    private val fakeErrorReporter = FakeErrorReporter()
    private val config = TestFactory.LINK_CONFIGURATION.copy(
        stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED.copy(
            amount = 1000L,
            currency = "usd"
        )
    )
    private val applicationId = "test.app.id"

    private fun createDefaultLinkAuth() = DefaultLinkAuth(
        linkGate = fakeLinkGate,
        linkRepository = fakeLinkRepository,
        integrityRequestManager = fakeIntegrityRequestManager,
        errorReporter = fakeErrorReporter,
        config = config,
        applicationId = applicationId
    )

    @Test
    fun `lookup validates required parameters`() = runTest {
        val linkAuth = createDefaultLinkAuth()

        // Test with no email and no auth intent
        val result = linkAuth.lookup(
            email = null,
            emailSource = null,
            linkAuthIntentId = null,
            customerId = null,
            sessionId = "session_123"
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(result.exceptionOrNull()?.message).contains(
            "Either email+emailSource or linkAuthIntentId must be provided"
        )
    }

    @Test
    fun `lookup accepts email and emailSource`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        fakeLinkGate.setUseAttestationEndpoints(false)

        val result = linkAuth.lookup(
            email = "test@example.com",
            emailSource = EmailSource.USER_ACTION,
            linkAuthIntentId = null,
            customerId = null,
            sessionId = "session_123"
        )

        assertThat(result.isSuccess).isTrue()

        val lookupCall = fakeLinkRepository.awaitLookup()
        assertThat(lookupCall.email).isEqualTo("test@example.com")
        assertThat(lookupCall.linkAuthIntentId).isNull()
    }

    @Test
    fun `lookup accepts linkAuthIntentId`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        fakeLinkGate.setUseAttestationEndpoints(false)

        val result = linkAuth.lookup(
            email = null,
            emailSource = null,
            linkAuthIntentId = "auth_intent_123",
            customerId = "customer_123",
            sessionId = "session_123"
        )

        assertThat(result.isSuccess).isTrue()

        val lookupCall = fakeLinkRepository.awaitLookup()
        assertThat(lookupCall.email).isNull()
        assertThat(lookupCall.linkAuthIntentId).isEqualTo("auth_intent_123")
    }

    @Test
    fun `lookup uses attestation when gate enabled`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val verificationToken = "verification_token_123"

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeIntegrityRequestManager.requestResult = Result.success(verificationToken)

        val result = linkAuth.lookup(
            email = "test@example.com",
            emailSource = EmailSource.USER_ACTION,
            linkAuthIntentId = null,
            customerId = null,
            sessionId = "session_123"
        )

        assertThat(result.isSuccess).isTrue()

        val mobileLookupCall = fakeLinkRepository.awaitMobileLookup()
        assertThat(mobileLookupCall.verificationToken).isEqualTo(verificationToken)
        assertThat(mobileLookupCall.appId).isEqualTo(applicationId)
        assertThat(mobileLookupCall.email).isEqualTo("test@example.com")
        assertThat(mobileLookupCall.linkAuthIntentId).isNull()
        assertThat(mobileLookupCall.sessionId).isEqualTo("session_123")
        assertThat(mobileLookupCall.emailSource).isEqualTo(EmailSource.USER_ACTION)
    }

    @Test
    fun `lookup with attestation reports integrity manager errors`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val attestationError = AttestationError(
            errorType = AttestationError.ErrorType.API_NOT_AVAILABLE,
            message = "Integrity token failed"
        )

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeIntegrityRequestManager.requestResult = Result.failure(attestationError)

        val result = linkAuth.lookup(
            email = "test@example.com",
            emailSource = EmailSource.USER_ACTION,
            linkAuthIntentId = null,
            customerId = null,
            sessionId = "session_123"
        )

        assertThat(result.isFailure).isTrue()

        val errorCall = fakeErrorReporter.awaitCall()
        assertThat(errorCall.errorEvent).isEqualTo(
            ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_GET_INTEGRITY_TOKEN
        )
    }

    @Test
    fun `lookup with attestation reports backend attestation errors`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val verificationToken = "verification_token_123"
        val apiException = APIException(stripeError = StripeError(code = "link_failed_to_attest_request"))

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeLinkRepository.mobileLookupConsumerResult = Result.failure(apiException)
        fakeIntegrityRequestManager.requestResult = Result.success(verificationToken)

        val result = linkAuth.lookup(
            email = "test@example.com",
            emailSource = EmailSource.USER_ACTION,
            linkAuthIntentId = null,
            customerId = null,
            sessionId = "session_123"
        )

        assertThat(result.isFailure).isTrue()

        val errorCall = fakeErrorReporter.awaitCall()
        assertThat(errorCall.errorEvent).isEqualTo(
            ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_ATTEST_REQUEST
        )
    }

    @Test
    fun `signup uses regular endpoint when attestation disabled`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        fakeLinkGate.setUseAttestationEndpoints(false)

        val result = linkAuth.signup(
            email = "test@example.com",
            phoneNumber = "1234567890",
            country = "US",
            countryInferringMethod = "locale",
            name = "Test User",
            consentAction = SignUpConsentAction.Implied
        )

        assertThat(result.isSuccess).isTrue()
        // Note: FakeLinkRepository doesn't track regular consumerSignUp calls, only mobile ones
    }

    @Test
    fun `signup uses attestation when gate enabled`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val verificationToken = "verification_token_123"

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeIntegrityRequestManager.requestResult = Result.success(verificationToken)

        val result = linkAuth.signup(
            email = "test@example.com",
            phoneNumber = "1234567890",
            country = "US",
            countryInferringMethod = "locale",
            name = "Test User",
            consentAction = SignUpConsentAction.Implied
        )

        assertThat(result.isSuccess).isTrue()

        val mobileSignupCall = fakeLinkRepository.awaitMobileSignup()
        assertThat(mobileSignupCall.name).isEqualTo("Test User")
        assertThat(mobileSignupCall.email).isEqualTo("test@example.com")
        assertThat(mobileSignupCall.phoneNumber).isEqualTo("1234567890")
        assertThat(mobileSignupCall.country).isEqualTo("US")
        assertThat(mobileSignupCall.consentAction).isEqualTo(ConsumerSignUpConsentAction.Implied)
        assertThat(mobileSignupCall.verificationToken).isEqualTo(verificationToken)
        assertThat(mobileSignupCall.appId).isEqualTo(applicationId)
        assertThat(mobileSignupCall.amount).isEqualTo(1000L)
        assertThat(mobileSignupCall.currency).isEqualTo("usd")
        assertThat(mobileSignupCall.incentiveEligibilitySession).isNull()
    }

    @Test
    fun `signup with attestation handles null phone number`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val verificationToken = "verification_token_123"

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeIntegrityRequestManager.requestResult = Result.success(verificationToken)

        val result = linkAuth.signup(
            email = "test@example.com",
            phoneNumber = null,
            country = "US",
            countryInferringMethod = "locale",
            name = "Test User",
            consentAction = SignUpConsentAction.Implied
        )

        assertThat(result.isSuccess).isTrue()

        val mobileSignupCall = fakeLinkRepository.awaitMobileSignup()
        assertThat(mobileSignupCall.phoneNumber).isNull()
        assertThat(mobileSignupCall.country).isEqualTo("US")
    }

    @Test
    fun `signup with attestation handles null country`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val verificationToken = "verification_token_123"

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeIntegrityRequestManager.requestResult = Result.success(verificationToken)

        val result = linkAuth.signup(
            email = "test@example.com",
            phoneNumber = "1234567890",
            country = null,
            countryInferringMethod = "locale",
            name = "Test User",
            consentAction = SignUpConsentAction.Implied
        )

        assertThat(result.isSuccess).isTrue()

        val mobileSignupCall = fakeLinkRepository.awaitMobileSignup()
        assertThat(mobileSignupCall.phoneNumber).isEqualTo("1234567890")
        assertThat(mobileSignupCall.country).isNull()
    }

    @Test
    fun `signup with attestation reports errors`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val verificationToken = "verification_token_123"
        val apiException = APIException(stripeError = StripeError(code = "link_failed_to_attest_request"))

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeLinkRepository.mobileConsumerSignUpResult = Result.failure(apiException)
        fakeIntegrityRequestManager.requestResult = Result.success(verificationToken)

        val result = linkAuth.signup(
            email = "test@example.com",
            phoneNumber = "1234567890",
            country = "US",
            countryInferringMethod = "locale",
            name = "Test User",
            consentAction = SignUpConsentAction.Implied
        )

        assertThat(result.isFailure).isTrue()

        val errorCall = fakeErrorReporter.awaitCall()
        assertThat(errorCall.errorEvent).isEqualTo(
            ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_ATTEST_REQUEST
        )
    }

    @Test
    fun `non-attestation errors are not reported`() = runTest(dispatcher) {
        val linkAuth = createDefaultLinkAuth()
        val verificationToken = "verification_token_123"
        val genericException = RuntimeException("Generic error")

        fakeLinkGate.setUseAttestationEndpoints(true)
        fakeLinkRepository.mobileLookupConsumerResult = Result.failure(genericException)
        fakeIntegrityRequestManager.requestResult = Result.success(verificationToken)

        val result = linkAuth.lookup(
            email = "test@example.com",
            emailSource = EmailSource.USER_ACTION,
            linkAuthIntentId = null,
            customerId = null,
            sessionId = "session_123"
        )

        assertThat(result.isFailure).isTrue()

        // Verify no error was reported
        assertThat(fakeErrorReporter.getLoggedErrors()).isEmpty()
    }
}
