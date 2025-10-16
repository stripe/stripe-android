package com.stripe.android.attestation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class AttestationViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `attest should emit Success result when integrity request succeeds`() = runTest {
        val expectedToken = "success_token"
        val fakeIntegrityRequestManager = FakeIntegrityRequestManager().apply {
            requestResult = Result.success(expectedToken)
        }

        val viewModel = createViewModel(fakeIntegrityRequestManager)

        viewModel.attest()

        assertThat(fakeIntegrityRequestManager.awaitRequestTokenCall()).isNull()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(AttestationActivityResult.Success::class.java)
            val successResult = result as AttestationActivityResult.Success
            assertThat(successResult.token).isEqualTo(expectedToken)

            expectNoEvents()
        }
        fakeIntegrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `attest should emit Failed result when integrity request fails`() = runTest {
        val expectedError = IOException("Network error")
        val fakeIntegrityRequestManager = FakeIntegrityRequestManager().apply {
            requestResult = Result.failure(expectedError)
        }

        val viewModel = createViewModel(fakeIntegrityRequestManager)

        viewModel.attest()

        assertThat(fakeIntegrityRequestManager.awaitRequestTokenCall()).isNull()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(AttestationActivityResult.Failed::class.java)
            val failedResult = result as AttestationActivityResult.Failed
            assertThat(failedResult.error).isEqualTo(expectedError)

            expectNoEvents()
        }
        fakeIntegrityRequestManager.ensureAllEventsConsumed()
    }

    private fun createViewModel(
        integrityRequestManager: FakeIntegrityRequestManager
    ) = AttestationViewModel(
        integrityRequestManager = integrityRequestManager
    )
}
