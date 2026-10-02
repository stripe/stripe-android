package com.stripe.android.challenge.passive

import android.app.Activity
import android.text.InputType
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.TestActivity
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerActivity
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerArgs
import com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerViewModel
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
internal class PassiveChallengeWarmerFocusTest {

    @Test
    fun warmerWindowDoesNotGainFocusOverHostInput() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val hCaptchaService = BlockingHCaptchaService()
        val warmerResumed = CountDownLatch(1)
        val warmerGainedFocus = CountDownLatch(1)

        val lifecycleCallback = object : ActivityLifecycleCallback {
            override fun onActivityLifecycleChanged(activity: Activity?, stage: Stage?) {
                if (activity !is PassiveChallengeWarmerActivity) return

                when (stage) {
                    Stage.PRE_ON_CREATE -> {
                        activity.viewModelFactory = createViewModelFactory(hCaptchaService)
                    }
                    Stage.CREATED -> {
                        activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
                            if (hasFocus) {
                                warmerGainedFocus.countDown()
                            }
                        }
                    }
                    Stage.RESUMED -> {
                        warmerResumed.countDown()
                    }
                    else -> Unit
                }
            }
        }

        val lifecycleMonitor = ActivityLifecycleMonitorRegistry.getInstance()
        lifecycleMonitor.addLifecycleCallback(lifecycleCallback)

        try {
            ActivityScenario.launch(TestActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val cardField = EditText(activity).apply {
                        inputType = InputType.TYPE_CLASS_NUMBER
                        isFocusableInTouchMode = true
                    }
                    activity.setContentView(cardField)
                    assertThat(cardField.requestFocus()).isTrue()
                    assertThat(activity.hasWindowFocus()).isTrue()
                    activity.startActivity(PassiveChallengeWarmerActivity.createIntent(activity, ARGS))
                }

                assertThat(hCaptchaService.warmUpStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
                assertThat(warmerResumed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
                instrumentation.waitForIdleSync()
                assertWithMessage("Passive challenge warmer gained window focus")
                    .that(warmerGainedFocus.await(FOCUS_OBSERVATION_SECONDS, TimeUnit.SECONDS))
                    .isFalse()

                hCaptchaService.completeWarmUp()
            }
        } finally {
            hCaptchaService.completeWarmUp()
            lifecycleMonitor.removeLifecycleCallback(lifecycleCallback)
        }
    }

    private fun createViewModelFactory(
        hCaptchaService: HCaptchaService,
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PassiveChallengeWarmerViewModel(
                    passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
                    hCaptchaService = hCaptchaService,
                ) as T
            }
        }
    }

    private class BlockingHCaptchaService : HCaptchaService {
        val warmUpStarted = CountDownLatch(1)
        private val warmUpCompleted = CompletableDeferred<Unit>()

        override suspend fun warmUp(
            activity: FragmentActivity,
            siteKey: String,
            rqData: String?,
        ) {
            warmUpStarted.countDown()
            warmUpCompleted.await()
        }

        override suspend fun performPassiveHCaptcha(
            activity: FragmentActivity,
            siteKey: String,
            rqData: String?,
            tokenTimeoutSeconds: Int?,
        ): HCaptchaService.Result {
            error("Not expected to be called")
        }

        fun completeWarmUp() {
            warmUpCompleted.complete(Unit)
        }
    }

    private companion object {
        private const val FOCUS_OBSERVATION_SECONDS = 1L
        private const val TIMEOUT_SECONDS = 5L
        private val PASSIVE_CAPTCHA_PARAMS = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data",
            tokenTimeoutSeconds = null,
        )
        private val ARGS = PassiveChallengeWarmerArgs(
            passiveCaptchaParams = PASSIVE_CAPTCHA_PARAMS,
            apiConfiguration = ApiConfiguration.State(
                publishableKey = "pk_test_123",
                stripeAccountId = null,
            ),
            productUsage = listOf("PaymentSheet"),
        )
    }
}
