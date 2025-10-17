package com.stripe.android.attestation

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AttestationActivityContractTest {

    @Test
    fun `createIntent creates intent correctly with AttestationArgs`() {
        val contract = AttestationActivityContract()
        val args = AttestationActivityContract.Args(
            publishableKey = "pk_test_123",
            productUsage = setOf("PaymentSheet", "CustomerSheet")
        )

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, AttestationActivity.EXTRA_ARGS, AttestationArgs::class.java)
        }

        assertThat(intent.component?.className).isEqualTo(AttestationActivity::class.java.name)
        assertThat(intentArgs?.publishableKey).isEqualTo("pk_test_123")
        assertThat(intentArgs?.productUsage).containsExactly("PaymentSheet", "CustomerSheet")
    }

    @Test
    fun `parseResult with success result`() {
        val contract = AttestationActivityContract()
        val successResult = AttestationActivityResult.Success("test_token")

        val result = contract.parseResult(
            Activity.RESULT_OK,
            intent(successResult)
        )

        assertThat(result).isEqualTo(successResult)
        assertThat((result as AttestationActivityResult.Success).token).isEqualTo("test_token")
    }

    @Test
    fun `parseResult with failed result`() {
        val contract = AttestationActivityContract()
        val throwable = RuntimeException("Attestation verification failed")
        val failedResult = AttestationActivityResult.Failed(throwable)

        val result = contract.parseResult(
            Activity.RESULT_OK,
            intent(failedResult)
        )

        assertThat(result).isInstanceOf<AttestationActivityResult.Failed>()
        val parsedFailedResult = result as AttestationActivityResult.Failed
        assertThat(parsedFailedResult.error.message).isEqualTo(throwable.message)
    }

    @Test
    fun `parseResult with no result in intent returns failed with 'No result' message`() {
        val contract = AttestationActivityContract()

        val result = contract.parseResult(
            Activity.RESULT_OK,
            Intent()
        )

        assertThat(result).isInstanceOf<AttestationActivityResult.Failed>()
        val failedResult = result as AttestationActivityResult.Failed
        assertThat(failedResult.error.message).isEqualTo("No result received from AttestationActivity")
    }

    @Test
    fun `parseResult with null intent returns failed with 'No result' message`() {
        val contract = AttestationActivityContract()

        val result = contract.parseResult(
            Activity.RESULT_OK,
            null
        )

        assertThat(result).isInstanceOf<AttestationActivityResult.Failed>()
        val failedResult = result as AttestationActivityResult.Failed
        assertThat(failedResult.error.message).isEqualTo("No result received from AttestationActivity")
    }

    private fun intent(result: AttestationActivityResult): Intent {
        return Intent().putExtras(
            bundleOf(AttestationActivityContract.EXTRA_RESULT to result)
        )
    }
}
