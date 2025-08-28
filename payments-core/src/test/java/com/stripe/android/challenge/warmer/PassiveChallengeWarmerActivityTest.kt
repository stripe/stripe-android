package com.stripe.android.challenge.warmer

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.challenge.ChallengeFactory
import com.stripe.android.challenge.FakeHCaptchaService
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.testing.CoroutineTestRule
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
    fun `activity should finish after warmup completes`() = runTest {
        val scenario = launchActivityForResult()

        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(Activity.RESULT_OK)

        scenario.close()
    }

    @Test
    fun `createIntent should create proper intent with args`() {
        val passiveCaptchaParams = ChallengeFactory.passiveCaptchaParams()
        val args = PassiveChallengeWarmerArgs(passiveCaptchaParams)
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
        val passiveCaptchaParams = ChallengeFactory.passiveCaptchaParams()
        val args = PassiveChallengeWarmerArgs(passiveCaptchaParams)
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

    @Test
    fun `activity should finish when view model factory throws`() = runTest {
        val scenario = launchActivityForResult(
            viewModelFactory = createTestViewModelFactory {
                throw IllegalArgumentException("could not create vm")
            }
        )

        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(Activity.RESULT_OK)

        scenario.close()
    }

    @Test
    fun `activity should finish when warmUp throws`() = runTest {
        val hCaptchaService = FakeHCaptchaService().apply {
            warmUpResult = { throw IllegalArgumentException("warmUp failed") }
        }

        val scenario = launchActivityForResult(hCaptchaService)

        advanceUntilIdle()

        assertThat(scenario.getResult().resultCode).isEqualTo(Activity.RESULT_OK)

        scenario.close()
    }

    private fun launchActivityForResult(
        hCaptchaService: HCaptchaService = FakeHCaptchaService(),
        viewModelFactory: ViewModelProvider.Factory = createTestViewModelFactory(hCaptchaService)
    ) = injectableActivityScenario<PassiveChallengeWarmerActivity> {
        injectActivity {
            this.viewModelFactory = viewModelFactory
        }
    }.apply {
        launchForResult(createIntent())
    }

    private fun createIntent() = Intent(
        ApplicationProvider.getApplicationContext(),
        PassiveChallengeWarmerActivity::class.java
    ).apply {
        putExtra(PassiveChallengeWarmerActivity.EXTRA_ARGS, PassiveChallengeWarmerArgs(passiveCaptchaParams))
    }

    private fun createTestViewModelFactory(
        hCaptchaService: HCaptchaService = FakeHCaptchaService(),
        viewModelBuilder: () -> PassiveChallengeWarmerViewModel = {
            PassiveChallengeWarmerViewModel(
                passiveCaptchaParams = passiveCaptchaParams,
                hCaptchaService = hCaptchaService
            )
        }
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return viewModelBuilder() as T
            }
        }
    }

    companion object {
        private val passiveCaptchaParams = ChallengeFactory.passiveCaptchaParams()
    }
}
