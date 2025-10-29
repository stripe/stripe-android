package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.RadarOptionsFactory
import kotlin.test.Test

internal class RadarOptionsTest {

    @Test
    fun `toParamMap() with both parameters null returns empty map`() {
        val radarOptions = RadarOptionsFactory.create(
            hCaptchaToken = null,
            verificationObject = null
        )

        assertThat(radarOptions.toParamMap()).isEmpty()
    }

    @Test
    fun `toParamMap() with only hCaptchaToken returns expected map`() {
        val radarOptions = RadarOptionsFactory.create(
            verificationObject = null
        )

        assertThat(radarOptions.toParamMap()).isEqualTo(
            mapOf("hcaptcha_token" to "test_token")
        )
    }

    @Test
    fun `toParamMap() with only androidVerificationObject returns expected map`() {
        val radarOptions = RadarOptionsFactory.create(
            hCaptchaToken = null
        )

        assertThat(radarOptions.toParamMap()).isEqualTo(
            mapOf(
                "android_verification_object" to mapOf(
                    "android_verification_token" to "test_verification_token"
                )
            )
        )
    }

    @Test
    fun `toParamMap() with both parameters returns expected map`() {
        val radarOptions = RadarOptionsFactory.create()

        assertThat(radarOptions.toParamMap()).isEqualTo(
            mapOf(
                "hcaptcha_token" to "test_token",
                "android_verification_object" to mapOf(
                    "android_verification_token" to "test_verification_token"
                )
            )
        )
    }
}
