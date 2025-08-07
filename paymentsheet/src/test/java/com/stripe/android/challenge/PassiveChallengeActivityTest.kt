package com.stripe.android.challenge

import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PassiveCaptchaParams
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeActivityTest {

    @Test
    fun `onCreate should immediately dismiss with NotImplementedError`() {
        val args = PassiveChallengeArgs(
            passiveCaptchaParams = PassiveCaptchaParams(
                siteKey = "test_site_key",
                rqData = "test_rq_data"
            )
        )
        val intent = PassiveChallengeActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            args
        )

        ActivityScenario.launchActivityForResult<PassiveChallengeActivity>(intent).use { scenario ->
            val scenarioResult = scenario.result

            assertThat(scenarioResult.resultCode).isEqualTo(PassiveChallengeActivity.RESULT_COMPLETE)

            val resultIntent = scenarioResult.resultData
            assertThat(resultIntent).isNotNull()

            val result = resultIntent.extras?.let {
                BundleCompat.getParcelable(
                    it,
                    PassiveChallengeActivityContract.EXTRA_RESULT,
                    PassiveChallengeActivityResult::class.java
                )
            }

            assertThat(result).isInstanceOf<PassiveChallengeActivityResult.Failed>()
            val failedResult = result as PassiveChallengeActivityResult.Failed
            assertThat(failedResult.error).isInstanceOf<NotImplementedError>()
            assertThat(failedResult.error.message).isEqualTo("Passive challenges not implemented yet")
        }
    }

    @Test
    fun `createIntent should create proper intent with args`() {
        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )
        val args = PassiveChallengeArgs(passiveCaptchaParams)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val intent = PassiveChallengeActivity.createIntent(context, args)

        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, PassiveChallengeActivity.EXTRA_ARGS, PassiveChallengeArgs::class.java)
        }
        assertThat(intentArgs).isEqualTo(args)
        assertThat(intentArgs?.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `getArgs should return args from SavedStateHandle when present`() {
        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )
        val args = PassiveChallengeArgs(passiveCaptchaParams)
        val savedStateHandle = SavedStateHandle().apply {
            set(PassiveChallengeActivity.EXTRA_ARGS, args)
        }

        val retrievedArgs = PassiveChallengeActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isEqualTo(args)
        assertThat(retrievedArgs?.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `getArgs should return null when args not present in SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()

        val retrievedArgs = PassiveChallengeActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isNull()
    }
}
