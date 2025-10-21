package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class AndroidVerificationObjectTest {

    @Test
    fun `toParamMap() with non-null token returns expected map`() {
        val androidVerificationObject = AndroidVerificationObject(
            androidVerificationToken = "test_verification_token_12345"
        )

        assertThat(androidVerificationObject.toParamMap()).isEqualTo(
            mapOf("android_verification_token" to "test_verification_token_12345")
        )
    }

    @Test
    fun `toParamMap() with null token returns empty map`() {
        val androidVerificationObject = AndroidVerificationObject(
            androidVerificationToken = null
        )

        assertThat(androidVerificationObject.toParamMap()).isEmpty()
    }
}
