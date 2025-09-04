package com.stripe.android.challenge

import androidx.fragment.app.FragmentActivity
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeWarmerViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val fakeHCaptchaService = FakeHCaptchaService()
    private val fakeActivity = object : FragmentActivity() {}

    private val testPassiveCaptchaParams = PassiveCaptchaParams(
        siteKey = "test_site_key",
        rqData = "test_rq_data"
    )

    @Test
    fun `warmUpPassiveChallenge should emit Success result when warmUp succeeds`() = runTest {
        fakeHCaptchaService.warmUpResult = { }

        val viewModel = createViewModel()

        viewModel.warmUpPassiveChallenge(fakeActivity)

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(PassiveChallengeWarmerResult.Success::class.java)

            expectNoEvents()
        }
    }

    @Test
    fun `warmUpPassiveChallenge should emit Failed result when warmUp throws exception`() = runTest {
        val expectedError = IOException("Network error")
        fakeHCaptchaService.warmUpResult = { throw expectedError }

        val viewModel = createViewModel()

        viewModel.warmUpPassiveChallenge(fakeActivity)

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(PassiveChallengeWarmerResult.Failed::class.java)
            val failedResult = result as PassiveChallengeWarmerResult.Failed
            assertThat(failedResult.error).isEqualTo(expectedError)

            expectNoEvents()
        }
    }

    @Test
    fun `warmUpPassiveChallenge should pass correct parameters to HCaptchaService`() = runTest {
        val hCaptchaService = FakeHCaptchaService()
        hCaptchaService.warmUpResult = { }

        val viewModel = createViewModel(
            passiveCaptchaParams = testPassiveCaptchaParams,
            hCaptchaService = hCaptchaService
        )

        viewModel.warmUpPassiveChallenge(fakeActivity)

        val warmUpCall = hCaptchaService.awaitWarmUpCall()
        assertThat(warmUpCall.siteKey).isEqualTo(testPassiveCaptchaParams.siteKey)
        assertThat(warmUpCall.rqData).isEqualTo(testPassiveCaptchaParams.rqData)
        assertThat(warmUpCall.activity).isEqualTo(fakeActivity)
    }

    private fun createViewModel(
        passiveCaptchaParams: PassiveCaptchaParams = testPassiveCaptchaParams,
        hCaptchaService: HCaptchaService = fakeHCaptchaService
    ) = PassiveChallengeWarmerViewModel(
        passiveCaptchaParams = passiveCaptchaParams,
        hCaptchaService = hCaptchaService
    )
}
