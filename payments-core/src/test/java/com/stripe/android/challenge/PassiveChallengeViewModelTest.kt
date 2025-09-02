package com.stripe.android.challenge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
internal class PassiveChallengeViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val fakeHCaptchaService = FakeHCaptchaService()

    private val testPassiveCaptchaParams = PassiveCaptchaParams(
        siteKey = "test_site_key",
        rqData = "test_rq_data"
    )

    @Test
    fun `startPassiveChallenge should emit Success result when HCaptcha succeeds`() = runTest {
        val expectedToken = "success_token"
        fakeHCaptchaService.result = HCaptchaService.Result.Success(expectedToken)

        val viewModel = createViewModel()

        viewModel.startPassiveChallenge()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(PassiveChallengeActivityResult.Success::class.java)
            val successResult = result as PassiveChallengeActivityResult.Success
            assertThat(successResult.token).isEqualTo(expectedToken)

            expectNoEvents()
        }
    }

    @Test
    fun `startPassiveChallenge should emit Failed result when HCaptcha fails`() = runTest {
        val expectedError = IOException("Network error")
        fakeHCaptchaService.result = HCaptchaService.Result.Failure(expectedError)

        val viewModel = createViewModel()

        viewModel.startPassiveChallenge()

        viewModel.result.test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(PassiveChallengeActivityResult.Failed::class.java)
            val failedResult = result as PassiveChallengeActivityResult.Failed
            assertThat(failedResult.error).isEqualTo(expectedError)

            expectNoEvents()
        }
    }

    @Test
    fun `startPassiveChallenge should pass correct parameters to HCaptchaService`() = runTest {
        val hCaptchaService = FakeHCaptchaService()
        hCaptchaService.result = HCaptchaService.Result.Success("token")
        val context: Context = ApplicationProvider.getApplicationContext()

        val viewModel = createViewModel(
            passiveCaptchaParams = testPassiveCaptchaParams,
            hCaptchaService = hCaptchaService
        )

        viewModel.startPassiveChallenge()

        val passiveCaptcha = hCaptchaService.awaitCall()
        assertThat(passiveCaptcha.siteKey).isEqualTo(passiveCaptcha.siteKey)
        assertThat(passiveCaptcha.rqData).isEqualTo(passiveCaptcha.rqData)
        assertThat(passiveCaptcha.context).isEqualTo(context)
    }

    private fun createViewModel(
        passiveCaptchaParams: PassiveCaptchaParams = testPassiveCaptchaParams,
        hCaptchaService: HCaptchaService = fakeHCaptchaService,
        context: Context = ApplicationProvider.getApplicationContext()
    ) = PassiveChallengeViewModel(
        passiveCaptchaParams = passiveCaptchaParams,
        hCaptchaService = hCaptchaService,
        context = context
    )
}
