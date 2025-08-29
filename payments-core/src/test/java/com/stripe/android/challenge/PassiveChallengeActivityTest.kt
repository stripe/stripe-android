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
internal class PassiveChallengeActivityTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `activity should dismiss with success result when challenge succeeds`() = runTest {
        val hCaptchaService = FakeHCaptchaService().apply {
            result = HCaptchaService.Result.Success("token")
        }

        val scenario = launchActivityForResult(hCaptchaService)
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(PassiveChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<PassiveChallengeActivityResult.Success>()
        val successResult = result as PassiveChallengeActivityResult.Success
        assertThat(successResult.token).isEqualTo("token")

        scenario.close()
    }

    @Test
    fun `activity should dismiss with failed result when challenge fails`() = runTest {
        val testError = Exception("Challenge failed")
        val hCaptchaService = FakeHCaptchaService().apply {
            result = HCaptchaService.Result.Failure(testError)
        }

        val scenario = launchActivityForResult(hCaptchaService)
        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(PassiveChallengeActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<PassiveChallengeActivityResult.Failed>()
        val failedResult = result as PassiveChallengeActivityResult.Failed
        assertThat(failedResult.error).isEqualTo(testError)

        scenario.close()
    }

    @Test
    fun `createIntent should create proper intent with args`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = PassiveChallengeActivity.createIntent(context, args)

        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, PassiveChallengeActivity.EXTRA_ARGS, PassiveChallengeArgs::class.java)
        }
        assertThat(intentArgs).isEqualTo(args)
        assertThat(intentArgs?.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `getArgs should return args from SavedStateHandle when present`() {
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

    private fun launchActivityForResult(
        hCaptchaService: HCaptchaService = FakeHCaptchaService(),
    ) = injectableActivityScenario<PassiveChallengeActivity> {
        injectActivity {
            viewModelFactory = createTestViewModelFactory(hCaptchaService)
        }
    }.apply {
        launchForResult(createIntent())
    }

    private fun createIntent() = Intent(
        ApplicationProvider.getApplicationContext(),
        PassiveChallengeActivity::class.java
    ).apply {
        putExtra(PassiveChallengeActivity.EXTRA_ARGS, args)
    }

    private fun extractActivityResult(
        scenario: InjectableActivityScenario<PassiveChallengeActivity>
    ): PassiveChallengeActivityResult? {
        return scenario.getResult().resultData?.extras?.let {
            BundleCompat.getParcelable(
                it,
                PassiveChallengeActivityContract.EXTRA_RESULT,
                PassiveChallengeActivityResult::class.java
            )
        }
    }

    private fun createTestViewModelFactory(
        hCaptchaService: HCaptchaService = FakeHCaptchaService(),
        context: Context = ApplicationProvider.getApplicationContext()
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PassiveChallengeViewModel(
                    passiveCaptchaParams = passiveCaptchaParams,
                    hCaptchaService = hCaptchaService,
                    context = context
                ) as T
            }
        }
    }

    companion object {
        private val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )

        private val args = PassiveChallengeArgs(
            passiveCaptchaParams = passiveCaptchaParams,
            publishableKey = "pk_123",
            productUsage = listOf("PaymentSheet")
        )
    }
}
