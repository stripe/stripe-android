package com.stripe.android.challenge

import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PassiveCaptchaParams
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
internal class PassiveChallengeWarmerActivityTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `activity should dismiss with success result when warmUp succeeds`() = runTest {
        val hCaptchaService = FakeHCaptchaService().apply {
            warmUpResult = { }
        }

        val scenario = launchActivityForResult(hCaptchaService)
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(PassiveChallengeWarmerActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<PassiveChallengeWarmerResult.Success>()

        scenario.close()
    }

    @Test
    fun `activity should dismiss with failed result when warmUp fails`() = runTest {
        val testError = Exception("WarmUp failed")
        val hCaptchaService = FakeHCaptchaService().apply {
            warmUpResult = { throw testError }
        }

        val scenario = launchActivityForResult(hCaptchaService)
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(PassiveChallengeWarmerActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<PassiveChallengeWarmerResult.Failed>()
        val failedResult = result as PassiveChallengeWarmerResult.Failed
        assertThat(failedResult.error).isEqualTo(testError)

        scenario.close()
    }

    @Test
    fun `createIntent should create proper intent with args`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = PassiveChallengeWarmerActivity.createIntent(context, args)

        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(
                it,
                PassiveChallengeWarmerActivity.EXTRA_ARGS,
                PassiveChallengeWarmerArgs::class.java
            )
        }
        assertThat(intentArgs).isEqualTo(args)
        assertThat(intentArgs?.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `getArgs should return args from SavedStateHandle when present`() {
        val savedStateHandle = SavedStateHandle().apply {
            set(PassiveChallengeWarmerActivity.EXTRA_ARGS, args)
        }

        val retrievedArgs = PassiveChallengeWarmerActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isEqualTo(args)
        assertThat(retrievedArgs?.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `getArgs should return null when args not present in SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()

        val retrievedArgs = PassiveChallengeWarmerActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isNull()
    }

    private fun launchActivityForResult(
        hCaptchaService: HCaptchaService = FakeHCaptchaService(),
    ) = injectableActivityScenario<PassiveChallengeWarmerActivity> {
        injectActivity {
            viewModelFactory = createTestViewModelFactory(hCaptchaService)
        }
    }.apply {
        launchForResult(createIntent())
    }

    private fun createIntent() = Intent(
        ApplicationProvider.getApplicationContext(),
        PassiveChallengeWarmerActivity::class.java
    ).apply {
        putExtra(PassiveChallengeWarmerActivity.EXTRA_ARGS, args)
    }

    private fun extractActivityResult(
        scenario: InjectableActivityScenario<PassiveChallengeWarmerActivity>
    ): PassiveChallengeWarmerResult? {
        return scenario.getResult().resultData?.extras?.let {
            BundleCompat.getParcelable(
                it,
                PassiveChallengeWarmerContract.EXTRA_RESULT,
                PassiveChallengeWarmerResult::class.java
            )
        }
    }

    private fun createTestViewModelFactory(
        hCaptchaService: HCaptchaService = FakeHCaptchaService()
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PassiveChallengeWarmerViewModel(
                    passiveCaptchaParams = passiveCaptchaParams,
                    hCaptchaService = hCaptchaService
                ) as T
            }
        }
    }

    companion object {
        private val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )

        private val args = PassiveChallengeWarmerArgs(
            passiveCaptchaParams = passiveCaptchaParams,
            publishableKey = "pk_123",
            productUsage = listOf("PaymentSheet")
        )
    }
}
