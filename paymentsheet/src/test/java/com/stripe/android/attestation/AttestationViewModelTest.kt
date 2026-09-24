package com.stripe.android.attestation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.attestation.analytics.FakeAttestationAnalyticsEventsReporter
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
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
        val fakeAttestationTokenProvider = FakeAttestationTokenProvider(Result.success(expectedToken))
        val fakeAnalyticsReporter = FakeAttestationAnalyticsEventsReporter()

        val viewModel = createViewModel(fakeAttestationTokenProvider, fakeAnalyticsReporter)

        fakeAttestationTokenProvider.awaitGetTokenCall()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(AttestationActivityResult.Success::class.java)
            val successResult = result as AttestationActivityResult.Success
            assertThat(successResult.token).isEqualTo(expectedToken)

            expectNoEvents()
        }
        fakeAttestationTokenProvider.ensureAllEventsConsumed()

        // Verify analytics events
        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestToken)
        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestTokenSucceeded)
        fakeAnalyticsReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `attest should emit Failed result when integrity request fails`() = runTest {
        val expectedError = IOException("Network error")
        val fakeAttestationTokenProvider = FakeAttestationTokenProvider(Result.failure(expectedError))
        val fakeAnalyticsReporter = FakeAttestationAnalyticsEventsReporter()

        val viewModel = createViewModel(fakeAttestationTokenProvider, fakeAnalyticsReporter)

        fakeAttestationTokenProvider.awaitGetTokenCall()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(AttestationActivityResult.Failed::class.java)

            expectNoEvents()
        }
        fakeAttestationTokenProvider.ensureAllEventsConsumed()

        // Verify analytics events
        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestToken)
        assertThat(fakeAnalyticsReporter.awaitCall())
            .isEqualTo(FakeAttestationAnalyticsEventsReporter.Call.RequestTokenFailed(expectedError))
        fakeAnalyticsReporter.ensureAllEventsConsumed()
    }

    private fun createViewModel(
        attestationTokenProvider: FakeAttestationTokenProvider,
        attestationAnalyticsEventsReporter: FakeAttestationAnalyticsEventsReporter =
            FakeAttestationAnalyticsEventsReporter()
    ) = AttestationViewModel(
        attestationTokenProvider = attestationTokenProvider,
        workContext = testDispatcher,
        attestationAnalyticsEventsReporter = attestationAnalyticsEventsReporter,
        errorReporter = FakeErrorReporter()
    ).also { viewModelStoreRule.track(it) }
}
