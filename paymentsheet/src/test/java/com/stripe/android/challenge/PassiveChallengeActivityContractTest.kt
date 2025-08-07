package com.stripe.android.challenge

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PassiveCaptchaParams
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeActivityContractTest {

    private val contract = PassiveChallengeActivityContract()

    @Test
    fun `createIntent creates intent correctly with PassiveChallengeArgs`() {
        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )
        val args = PassiveChallengeActivityContract.Args(passiveCaptchaParams)

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, PassiveChallengeActivity.EXTRA_ARGS, PassiveChallengeArgs::class.java)
        }

        assertThat(intent.component?.className).isEqualTo(PassiveChallengeActivity::class.java.name)
        assertThat(intentArgs?.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `parseResult with success result`() {
        val successResult = PassiveChallengeActivityResult.Success("test_token")
        val result = contract.parseResult(
            Activity.RESULT_OK,
            intent(successResult)
        )
        assertThat(result).isEqualTo(successResult)
        assertThat((result as PassiveChallengeActivityResult.Success).token).isEqualTo("test_token")
    }

    @Test
    fun `parseResult with failed result`() {
        val throwable = RuntimeException("Captcha verification failed")
        val failedResult = PassiveChallengeActivityResult.Failed(throwable)
        val result = contract.parseResult(
            Activity.RESULT_OK,
            intent(failedResult)
        )
        assertThat(result).isInstanceOf<PassiveChallengeActivityResult.Failed>()
        val parsedFailedResult = result as PassiveChallengeActivityResult.Failed
        assertThat(parsedFailedResult.error.message).isEqualTo(throwable.message)
    }

    @Test
    fun `parseResult with no result in intent returns failed with 'No result' message`() {
        val result = contract.parseResult(
            Activity.RESULT_OK,
            Intent()
        )
        assertThat(result).isInstanceOf<PassiveChallengeActivityResult.Failed>()
        val failedResult = result as PassiveChallengeActivityResult.Failed
        assertThat(failedResult.error.message).isEqualTo("No result")
    }

    @Test
    fun `parseResult with null intent returns failed with 'No result' message`() {
        val result = contract.parseResult(
            Activity.RESULT_OK,
            null
        )
        assertThat(result).isInstanceOf<PassiveChallengeActivityResult.Failed>()
        val failedResult = result as PassiveChallengeActivityResult.Failed
        assertThat(failedResult.error.message).isEqualTo("No result")
    }

    private fun intent(result: PassiveChallengeActivityResult): Intent {
        return Intent().putExtras(
            bundleOf(PassiveChallengeActivityContract.EXTRA_RESULT to result)
        )
    }
}
