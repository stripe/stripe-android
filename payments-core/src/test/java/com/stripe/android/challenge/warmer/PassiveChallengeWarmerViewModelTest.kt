package com.stripe.android.challenge.warmer

import androidx.fragment.app.FragmentActivity
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.ChallengeFactory
import com.stripe.android.challenge.FakeHCaptchaService
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeWarmerViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `warmUp should call HCaptchaService warmUp with correct parameters`() = runTest {
        val fakeHCaptchaService = FakeHCaptchaService()
        val fakeActivity = object : FragmentActivity() {}
        val testPassiveCaptchaParams = ChallengeFactory.passiveCaptchaParams()
        val viewModel = createViewModel(
            passiveCaptchaParams = testPassiveCaptchaParams,
            hCaptchaService = fakeHCaptchaService
        )

        viewModel.warmUp(fakeActivity)

        val warmUpCall = fakeHCaptchaService.awaitWarmUpCall()

        assertThat(warmUpCall.siteKey).isEqualTo(testPassiveCaptchaParams.siteKey)
        assertThat(warmUpCall.rqData).isEqualTo(testPassiveCaptchaParams.rqData)
        assertThat(warmUpCall.activity).isEqualTo(fakeActivity)
        fakeHCaptchaService.ensureWarmUpCallsConsumed()
    }

    private fun createViewModel(
        passiveCaptchaParams: PassiveCaptchaParams = ChallengeFactory.passiveCaptchaParams(),
        hCaptchaService: HCaptchaService = FakeHCaptchaService()
    ) = PassiveChallengeWarmerViewModel(
        passiveCaptchaParams = passiveCaptchaParams,
        hCaptchaService = hCaptchaService
    )
}
