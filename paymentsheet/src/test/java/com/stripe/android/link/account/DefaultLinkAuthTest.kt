package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class DefaultLinkAuthTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

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
    fun `sign in attempt with attestation failure returns AttestationFailed`() = runTest {
        val error = APIException(
            stripeError = StripeError(
                code = "link_failed_to_attest_request"
            )
        )
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

        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()

    }

    @Test
    fun `sign in attempt with token fetch failure returns AttestationFailed`() = runTest {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.INTERNAL_ERROR,
            message = "oops"
        )
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

        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()

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
            emailSource = TestFactory.EMAIL_SOURCE
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

        integrityRequestManager.requestResult = Result.failure(error)

        val linkAuth = linkAuth(
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE
        )

        integrityRequestManager.awaitRequestTokenCall()

        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `lookup attempt with token fetch failure returns AttestationFailed`() = runTest {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.INTERNAL_ERROR,
            message = "oops"
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
            emailSource = TestFactory.EMAIL_SOURCE
        )

        integrityRequestManager.awaitRequestTokenCall()

        assertThat(result).isEqualTo(LinkAuthResult.AttestationFailed(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
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
            emailSource = TestFactory.EMAIL_SOURCE
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

        val linkAuth = linkAuth(
            useAttestationEndpoints = false,
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager
        )

        val result = linkAuth.lookUp(
            email = TestFactory.CUSTOMER_EMAIL,
            emailSource = TestFactory.EMAIL_SOURCE
        )

        val accountManagerCall = linkAccountManager.awaitLookupCall()

        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.startSession).isTrue()

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
            emailSource = TestFactory.EMAIL_SOURCE
        )

        val accountManagerCall = linkAccountManager.awaitLookupCall()

        assertThat(accountManagerCall.email).isEqualTo(TestFactory.CUSTOMER_EMAIL)
        assertThat(accountManagerCall.startSession).isTrue()

        assertThat(result).isEqualTo(LinkAuthResult.Error(error))

        linkAccountManager.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()

    }

    private fun linkAuth(
        useAttestationEndpoints: Boolean = true,
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager()
    ): DefaultLinkAuth {
        return DefaultLinkAuth(
            linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
                useAttestationEndpointsForLink = useAttestationEndpoints
            ),
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            applicationId = TestFactory.APP_ID
        )
    }
}