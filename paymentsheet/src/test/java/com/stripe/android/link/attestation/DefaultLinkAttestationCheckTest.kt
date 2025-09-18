package com.stripe.android.link.attestation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class DefaultLinkAttestationCheckTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val testRule = CoroutineTestRule(dispatcher)

    @Test
    fun `attestation check should be successful when useAttestationEndpoints is false`() = runTest {
        val linkGate = FakeLinkGate()
        linkGate.setUseAttestationEndpoints(false)

        val attestationCheck = attestationCheck(linkGate = linkGate)

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.Successful)
    }

    @Test
    fun `attestation check should be successful when there is no email for lookup`() = runTest {
        val linkGate = FakeLinkGate()
        val linkAccountManager = FakeLinkAccountManager()
        linkGate.setUseAttestationEndpoints(true)
        linkAccountManager.setLinkAccount(LinkAccountUpdate.Value(null))

        val attestationCheck = attestationCheck(
            linkGate = linkGate,
            linkAccountManager = linkAccountManager,
            linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
                customerInfo = TestFactory.LINK_CUSTOMER_INFO.copy(
                    email = null
                )
            )
        )

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.Successful)
    }

    @Test
    fun `attestation check should return AttestationFailed when integrity preparation fails`() = runTest {
        val error = Throwable("oops")
        val errorReporter = FakeErrorReporter()
        val integrityRequestManager = FakeIntegrityRequestManager()
        integrityRequestManager.prepareResult = Result.failure(error)

        val attestationCheck = attestationCheck(
            integrityRequestManager = integrityRequestManager,
            errorReporter = errorReporter
        )

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.AttestationFailed(error))
        assertThat(errorReporter.getLoggedErrors())
            .containsExactly(
                ErrorReporter.ExpectedErrorEvent.LINK_NATIVE_FAILED_TO_PREPARE_INTEGRITY_MANAGER.eventName
            )
    }

    @Test
    fun `attestation check should return AttestationFailed when lookup returns AttestationError`() = runTest {
        val error = AttestationError(AttestationError.ErrorType.NETWORK_ERROR, "Device not recognized")
        val linkGate = FakeLinkGate()
        val linkAccountManager = FakeLinkAccountManager()

        linkAccountManager.lookupResult = Result.failure(error)

        val attestationCheck = attestationCheck(
            linkGate = linkGate,
            linkAccountManager = linkAccountManager,
        )

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.AttestationFailed(error))
    }

    @Test
    fun `attestation check should return AttestationFailed when lookup returns backend attestation error`() = runTest {
        val stripeError = StripeError(code = "link_failed_to_attest_request", message = "Failed to attest request")
        val error = APIException(stripeError = stripeError)
        val linkGate = FakeLinkGate()
        val linkAccountManager = FakeLinkAccountManager()

        linkAccountManager.lookupResult = Result.failure(error)

        val attestationCheck = attestationCheck(
            linkGate = linkGate,
            linkAccountManager = linkAccountManager,
        )

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.AttestationFailed(error))
    }

    @Test
    fun `attestation check should return AccountError when lookup returns account error`() = runTest {
        val stripeError =
            StripeError(code = "link_consumer_details_not_available", message = "Consumer details not available")
        val error = APIException(stripeError = stripeError)
        val linkGate = FakeLinkGate()
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.failure(error)

        val attestationCheck = attestationCheck(linkGate = linkGate, linkAccountManager = linkAccountManager)

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.AccountError(error))
    }

    @Test
    fun `attestation check should return Error when lookup returns generic error`() = runTest {
        val error = RuntimeException("Generic network error")
        val linkGate = FakeLinkGate()
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.failure(error)

        val attestationCheck = attestationCheck(linkGate = linkGate, linkAccountManager = linkAccountManager)

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.Error(error))
    }

    @Test
    fun `attestation check should return Successful when lookup returns NoLinkAccountFound`() = runTest {
        val linkGate = FakeLinkGate()
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(null)

        val attestationCheck = attestationCheck(linkGate = linkGate, linkAccountManager = linkAccountManager)

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.Successful)
    }

    @Test
    fun `attestation check should return Successful when lookup returns success`() = runTest {
        val linkGate = FakeLinkGate()
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.lookupResult = Result.success(TestFactory.LINK_ACCOUNT)

        val attestationCheck = attestationCheck(linkGate = linkGate, linkAccountManager = linkAccountManager)

        assertThat(attestationCheck.invoke())
            .isEqualTo(LinkAttestationCheck.Result.Successful)
    }

    private fun attestationCheck(
        linkGate: LinkGate = FakeLinkGate(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager(),
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        linkConfiguration: LinkConfiguration = TestFactory.LINK_CONFIGURATION,
    ): DefaultLinkAttestationCheck {
        return DefaultLinkAttestationCheck(
            linkGate = linkGate,
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            linkConfiguration = linkConfiguration,
            errorReporter = errorReporter,
            workContext = dispatcher.scheduler
        )
    }
}
