package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ElementsSessionClientParamsTest {

    @Test
    fun `toCheckoutSessionMap contains expected keys and values`() {
        val params = ElementsSessionClientParams(
            mobileAppId = "com.example.app",
            mobileSessionIdProvider = { "session_123" },
        )

        val map = params.toCheckoutSessionMap()

        assertThat(map).containsEntry("is_aggregation_expected", "true")
        assertThat(map).containsEntry("locale", params.locale)
        assertThat(map).containsEntry("mobile_session_id", "session_123")
        assertThat(map).containsEntry("mobile_app_id", "com.example.app")
        assertThat(map).hasSize(4)
    }

    @Test
    fun `mobileSessionId delegates to provider`() {
        var callCount = 0
        val params = ElementsSessionClientParams(
            mobileAppId = "com.example.app",
            mobileSessionIdProvider = {
                callCount++
                "session_$callCount"
            },
        )

        assertThat(params.mobileSessionId).isEqualTo("session_1")
        assertThat(params.mobileSessionId).isEqualTo("session_2")
    }
}
