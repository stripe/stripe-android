package com.stripe.android.core.networking

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.version.StripeSdkVersion
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class StripeClientUserAgentHeaderFactoryTest {
    private val factory = StripeClientUserAgentHeaderFactory {
        "example_value"
    }

    @Test
    fun `createHeaderValue() should return expected JSON string`() {
        assertThat(
            factory.createHeaderValue().toString()
        ).isEqualTo(
            JSONObject(
                """
                {
                    "os.name": "android",
                    "os.version": "${Build.VERSION.SDK_INT}",
                    "bindings.version": "${StripeSdkVersion.VERSION_NAME}",
                    "lang": "Java",
                    "publisher": "Stripe",
                    "http.agent": "example_value"
                }
                """.trimIndent()
            ).toString()
        )
    }
}
