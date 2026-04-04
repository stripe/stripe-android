package com.stripe.android.core.networking

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.version.StripeSdkVersion
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
            buildJsonObject {
                put("os.name", "android")
                put("os.version", Build.VERSION.SDK_INT.toString())
                put("bindings.version", StripeSdkVersion.VERSION_NAME)
                put("lang", "Java")
                put("publisher", "Stripe")
                put("http.agent", "example_value")
            }.toString()
        )
    }
}
