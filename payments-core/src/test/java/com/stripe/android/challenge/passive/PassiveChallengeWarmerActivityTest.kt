package com.stripe.android.challenge.passive

import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerActivity
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerArgs
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerCompleted
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerContract
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerViewModel
import com.stripe.android.hcaptcha.FakeHCaptchaService
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
internal class PassiveChallengeWarmerActivityTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `activity should dismiss result after warmUp`() {
        runTest {
            val hCaptchaService = FakeHCaptchaService().apply {
                warmUpResult = { }
            }

            val scenario = launchActivityForResult(hCaptchaService)
            advanceUntilIdle()

            hCaptchaService.awaitWarmUpCall()

            assertThat(scenario.getResult().resultCode).isEqualTo(PassiveChallengeWarmerActivity.RESULT_COMPLETE)

            val result = extractActivityResult(scenario)
            assertThat(result).isInstanceOf<PassiveChallengeWarmerCompleted>()

            scenario.close()
            hCaptchaService.ensureAllEventsConsumed()
        }
    }

    @Test
    fun `activity should restart warmup on configuration change`() = runTest {
        val hCaptchaService = FakeHCaptchaService().apply {
            warmUpResult = {
                delay(5.seconds)
            }
        }

        val scenario = launchActivityForResult(hCaptchaService)

        hCaptchaService.awaitWarmUpCall()

        scenario.recreate()

        advanceUntilIdle()

        hCaptchaService.awaitWarmUpCall()

        assertThat(scenario.getResult().resultCode).isEqualTo(PassiveChallengeWarmerActivity.RESULT_COMPLETE)

        val result = extractActivityResult(scenario)
        assertThat(result).isInstanceOf<PassiveChallengeWarmerCompleted>()

        scenario.close()
        hCaptchaService.ensureAllEventsConsumed()
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
    ): PassiveChallengeWarmerCompleted? {
        return scenario.getResult().resultData?.extras?.let {
            BundleCompat.getParcelable(
                it,
                PassiveChallengeWarmerContract.EXTRA_RESULT,
                PassiveChallengeWarmerCompleted::class.java
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
