package com.stripe.hcaptcha

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class HCaptchaCompatTest {

    @Test
    fun testStoreValues() {
        val config = HCaptchaConfig(siteKey = "test")
        val internalConfig = HCaptchaInternalConfig()
        val listener = HCaptchaStateListener(
            onOpen = {},
            onSuccess = {},
            onFailure = {}
        )
        val bundle = HCaptchaCompat.storeValues(
            internalConfig = internalConfig,
            config = config,
            listener = listener
        )

        assertThat(HCaptchaCompat.getConfig(bundle)).isEqualTo(config)
        assertThat(HCaptchaCompat.getInternalConfig(bundle)).isEqualTo(internalConfig)
        assertThat(HCaptchaCompat.getStateListener(bundle)).isEqualTo(listener)
    }
}
