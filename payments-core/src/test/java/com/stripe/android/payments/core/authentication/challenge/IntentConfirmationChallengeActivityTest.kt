package com.stripe.android.payments.core.authentication.challenge

import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.isInstanceOf
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentConfirmationChallengeActivityTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `activity should dismiss with success result when hcaptcha challenge succeeds`() = runTest {
        val hCaptchaService = FakeHCaptchaService().apply {
            result = HCaptchaService.Result.Success("token")
        }

        val scenario = launchActivityForResult(hCaptchaService, createHCaptchaChallenge())
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Success>()
        val successResult = result as IntentConfirmationChallengeActivityResult.Success
        assertThat(successResult.token).isEqualTo("token")

        scenario.close()
    }

    @Test
    fun `activity should dismiss with failed result when hcaptcha challenge fails`() = runTest {
        val exception = RuntimeException("Challenge failed")
        val hCaptchaService = FakeHCaptchaService().apply {
            result = HCaptchaService.Result.Failure(exception)
        }

        val scenario = launchActivityForResult(hCaptchaService, createHCaptchaChallenge())
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Failed>()
        val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failedResult.error).isEqualTo(exception)

        scenario.close()
    }

    @Test
    fun `activity should fail when human security challenge is requested`() = runTest {
        val hCaptchaService = FakeHCaptchaService()

        val scenario = launchActivityForResult(hCaptchaService, createHumanSecurityChallenge())
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Failed>()
        val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failedResult.error).isInstanceOf<UnsupportedOperationException>()

        scenario.close()
    }

    @Test
    fun `activity should fail when arkose challenge is requested`() = runTest {
        val hCaptchaService = FakeHCaptchaService()

        val scenario = launchActivityForResult(hCaptchaService, createArkoseChallenge())
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(IntentConfirmationChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<IntentConfirmationChallengeActivityResult.Failed>()
        val failedResult = result as IntentConfirmationChallengeActivityResult.Failed
        assertThat(failedResult.error).isInstanceOf<UnsupportedOperationException>()

        scenario.close()
    }

    private fun launchActivityForResult(
        hCaptchaService: HCaptchaService,
        challenge: StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge = createHCaptchaChallenge()
    ): InjectableActivityScenario<IntentConfirmationChallengeActivity> {
        return injectableActivityScenario {
            IntentConfirmationChallengeActivity.createIntent(
                ApplicationProvider.getApplicationContext(),
                IntentConfirmationChallengeArgs(
                    intentConfirmationChallenge = challenge,
                    publishableKey = "pk_test_123",
                    productUsage = listOf("test")
                )
            )
        }.apply {
            inject { activity ->
                activity.viewModelFactory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FakeIntentConfirmationChallengeViewModel(
                            challenge,
                            hCaptchaService
                        ) as T
                    }
                }
            }
        }
    }

    private fun extractActivityResult(
        scenario: InjectableActivityScenario<IntentConfirmationChallengeActivity>
    ): IntentConfirmationChallengeActivityResult {
        val intent = scenario.getResult().resultData
        return BundleCompat.getParcelable(
            intent.extras!!,
            IntentConfirmationChallengeActivityContract.EXTRA_RESULT,
            IntentConfirmationChallengeActivityResult::class.java
        )!!
    }

    private fun createHCaptchaChallenge() = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge(
        verificationUrl = "https://api.stripe.com/v1/payment_intents/pi_test123/confirm",
        vendorData = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.VendorData.HCaptchaVendorData(
            siteKey = "test-site-key",
            rqData = "test-rq-data"
        )
    )

    private fun createHumanSecurityChallenge() = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge(
        verificationUrl = "https://api.stripe.com/v1/payment_intents/pi_test123/confirm",
        vendorData = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.VendorData.HumanSecurityVendorData(
            uuid = "test-uuid",
            vid = "test-vid",
            appId = "test-app-id"
        )
    )

    private fun createArkoseChallenge() = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge(
        verificationUrl = "https://api.stripe.com/v1/payment_intents/pi_test123/confirm",
        vendorData = StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge.VendorData.ArkoseVendorData(
            blob = "test-blob-data"
        )
    )
}
