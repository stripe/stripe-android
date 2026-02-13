package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class AndroidVerificationObjectTest {

    @Test
    fun `toParamMap() returns expected map with token and appId`() {
        val androidVerificationObject = AndroidVerificationObject(
            androidVerificationToken = "test_verification_token_12345",
            appId = "com.stripe.app"
        )

        assertThat(androidVerificationObject.toParamMap()).isEqualTo(
            mapOf(
                "android_verification_token" to "test_verification_token_12345",
                "app_id" to "com.stripe.app"
            )
        )
    }
}
