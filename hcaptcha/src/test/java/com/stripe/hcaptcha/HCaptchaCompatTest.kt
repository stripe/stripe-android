package com.stripe.hcaptcha

import android.app.LauncherActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HCaptchaCompatTest {

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<LauncherActivity> = activityScenarioRule()

//    @Before
//    fun setup() {
//        Intents.init()
//    }
//
//    @After
//    fun cleanup() {
//        Intents.release()
//        BackgroundTaskTracker.reset()
//    }

    @Test
    fun testStore() {
        val config = HCaptchaConfig("")
        val internalConfig = HCaptchaInternalConfig()
        val listener = HCaptchaStateListener(
            onOpen = {},
            onSuccess = {},
            onFailure = {}
        )
        val bundle = HCaptchaCompat.storeValues(
            config = config,
            internalConfig = internalConfig,
            listener = listener
        )
        assertThat(bundle.size()).isEqualTo(3)
    }
}