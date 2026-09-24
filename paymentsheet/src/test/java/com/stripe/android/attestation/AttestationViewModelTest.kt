package com.stripe.android.attestation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.attestation.analytics.FakeAttestationAnalyticsEventsReporter
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.attestation.AttestationError
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class AttestationViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `attest should emit Success result when integrity request succeeds`() = runTest {
        val expectedToken = "success_token"
        val fakeIntegrityRequestManager = FakeIntegrityRequestManager().apply {
            requestResult = Result.success(expectedToken)
        }
        val fakeAnalyticsReporter = FakeAttestationAnalyticsEventsReporter()
        val fakeErrorReporter = FakeErrorReporter()

        val viewModel = createViewModel(
            integrityRequestManager = fakeIntegrityRequestManager,
            attestationAnalyticsEventsReporter = fakeAnalyticsReporter,
            errorReporter = fakeErrorReporter,
        )

        assertThat(fakeIntegrityRequestManager.awaitRequestTokenCall()).isNull()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(AttestationActivityResult.Success::class.java)
            val successResult = result as AttestationActivityResult.Success
            assertThat(successResult.token).isEqualTo(expectedToken)

            expectNoEvents()
        }
        fakeIntegrityRequestManager.ensureAllEventsConsumed()

        // Verify analytics events
        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestToken)
        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestTokenSucceeded)
        fakeAnalyticsReporter.ensureAllEventsConsumed()
        fakeErrorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `attest should report expected error when integrity request fails`() = runFailureScenario(
        error = IOException("Network error"),
        expectedErrorEvent = ErrorReporter.ExpectedErrorEvent
            .INTENT_CONFIRMATION_HANDLER_ATTESTATION_REQUEST_TOKEN_FAILED,
    )

    @Test
    fun `attest should report other attestation errors as expected`() = runFailureScenario(
        error = AttestationError(
            errorType = AttestationError.ErrorType.NETWORK_ERROR,
            message = "Network error",
        ),
        expectedErrorEvent = ErrorReporter.ExpectedErrorEvent
            .INTENT_CONFIRMATION_HANDLER_ATTESTATION_REQUEST_TOKEN_FAILED,
    )

    @Test
    fun `attest should report invalid cloud project number as unexpected`() = runFailureScenario(
        error = AttestationError(
            errorType = AttestationError.ErrorType.CLOUD_PROJECT_NUMBER_IS_INVALID,
            message = "Invalid cloud project number",
        ),
        expectedErrorEvent = ErrorReporter.UnexpectedErrorEvent
            .INTENT_CONFIRMATION_HANDLER_ATTESTATION_CLOUD_PROJECT_NUMBER_IS_INVALID,
    )

    @Test
    fun `attest should report invalid integrity token provider as unexpected`() = runFailureScenario(
        error = AttestationError(
            errorType = AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID,
            message = "Invalid integrity token provider",
        ),
        expectedErrorEvent = ErrorReporter.UnexpectedErrorEvent
            .INTENT_CONFIRMATION_HANDLER_ATTESTATION_INTEGRITY_TOKEN_PROVIDER_INVALID,
    )

    @Test
    fun `attest should report long request hash as unexpected`() = runFailureScenario(
        error = AttestationError(
            errorType = AttestationError.ErrorType.REQUEST_HASH_TOO_LONG,
            message = "Request hash is too long",
        ),
        expectedErrorEvent = ErrorReporter.UnexpectedErrorEvent
            .INTENT_CONFIRMATION_HANDLER_ATTESTATION_REQUEST_HASH_TOO_LONG,
    )

    private fun runFailureScenario(
        error: Throwable,
        expectedErrorEvent: ErrorReporter.ErrorEvent,
    ) = runTest {
        val fakeIntegrityRequestManager = FakeIntegrityRequestManager().apply {
            requestResult = Result.failure(error)
        }
        val fakeAnalyticsReporter = FakeAttestationAnalyticsEventsReporter()
        val fakeErrorReporter = FakeErrorReporter()

        val viewModel = createViewModel(
            integrityRequestManager = fakeIntegrityRequestManager,
            attestationAnalyticsEventsReporter = fakeAnalyticsReporter,
            errorReporter = fakeErrorReporter,
        )

        assertThat(fakeIntegrityRequestManager.awaitRequestTokenCall()).isNull()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(AttestationActivityResult.Failed::class.java)

            expectNoEvents()
        }
        fakeIntegrityRequestManager.ensureAllEventsConsumed()

        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestToken)
        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestTokenFailed(error))
        fakeAnalyticsReporter.ensureAllEventsConsumed()

        val reportCall = fakeErrorReporter.awaitCall()
        assertThat(reportCall.errorEvent).isEqualTo(expectedErrorEvent)
        assertThat(reportCall.stripeException?.cause).isSameInstanceAs(error)
        assertThat(reportCall.additionalNonPiiParams).isEmpty()
        fakeErrorReporter.ensureAllEventsConsumed()
    }

    private fun createViewModel(
        integrityRequestManager: FakeIntegrityRequestManager,
        attestationAnalyticsEventsReporter: FakeAttestationAnalyticsEventsReporter,
        errorReporter: ErrorReporter,
    ) = AttestationViewModel(
        integrityRequestManager = integrityRequestManager,
        workContext = testDispatcher,
        attestationAnalyticsEventsReporter = attestationAnalyticsEventsReporter,
        errorReporter = errorReporter,
    ).also { viewModelStoreRule.track(it) }
}
