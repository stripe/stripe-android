package com.stripe.android.networking

import com.google.common.truth.Truth.assertThat
import com.stripe.android.Stripe
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
                    "os.version": "30",
                    "bindings.version": "${Stripe.VERSION_NAME}",
                    "lang": "Java",
                    "publisher": "Stripe",
                    "http.agent": "example_value"
                }
                """.trimIndent()
            ).toString()
        )
    }
}
