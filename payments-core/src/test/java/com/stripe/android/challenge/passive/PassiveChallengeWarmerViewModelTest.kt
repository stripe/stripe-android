package com.stripe.android.challenge.passive

import androidx.fragment.app.FragmentActivity
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerViewModel
import com.stripe.android.hcaptcha.FakeHCaptchaService
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.ViewModelStoreTestRule
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeWarmerViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    private val fakeHCaptchaService = FakeHCaptchaService()
    private val fakeActivity = object : FragmentActivity() {}

    private val testPassiveCaptchaParams = PassiveCaptchaParams(
        siteKey = "test_site_key",
        rqData = "test_rq_data",
        tokenTimeoutSeconds = 30
    )

    @Test
    fun `warmUpPassiveChallenge should pass timeout to cacheState`() = runTest {
        val viewModel = createViewModel()
        val job = launch {
            viewModel.warmUpPassiveChallenge(fakeActivity)
        }

        val cacheStateCall = fakeHCaptchaService.awaitCacheStateCall()
        assertThat(cacheStateCall.timeoutSeconds).isEqualTo(testPassiveCaptchaParams.tokenTimeoutSeconds)
        fakeHCaptchaService.awaitWarmUpCall()

        job.cancelAndJoin()
        fakeHCaptchaService.ensureAllEventsConsumed()
    }

    @Test
    fun `warmUpPassiveChallenge should pass correct parameters to HCaptchaService`() = runTest {
        val hCaptchaService = FakeHCaptchaService()
        hCaptchaService.warmUpResult = { }

        val viewModel = createViewModel(
            passiveCaptchaParams = testPassiveCaptchaParams,
            hCaptchaService = hCaptchaService
        )

        val job = launch {
            viewModel.warmUpPassiveChallenge(fakeActivity)
        }

        hCaptchaService.awaitCacheStateCall()
        val warmUpCall = hCaptchaService.awaitWarmUpCall()
        assertThat(warmUpCall.siteKey).isEqualTo(testPassiveCaptchaParams.siteKey)
        assertThat(warmUpCall.rqData).isEqualTo(testPassiveCaptchaParams.rqData)
        assertThat(warmUpCall.activity).isEqualTo(fakeActivity)

        job.cancelAndJoin()
        hCaptchaService.ensureAllEventsConsumed()
    }

    private fun createViewModel(
        passiveCaptchaParams: PassiveCaptchaParams = testPassiveCaptchaParams,
        hCaptchaService: HCaptchaService = fakeHCaptchaService
    ) = PassiveChallengeWarmerViewModel(
        passiveCaptchaParams = passiveCaptchaParams,
        hCaptchaService = hCaptchaService
    ).also { viewModelStoreRule.track(it) }
}
