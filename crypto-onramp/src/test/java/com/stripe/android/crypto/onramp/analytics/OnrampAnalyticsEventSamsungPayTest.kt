package com.stripe.android.crypto.onramp.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnrampAnalyticsEventSamsungPayTest {
    @Test
    fun `initialized event matches analytics contract`() {
        val event = OnrampAnalyticsEvent.SamsungPayInitialized

        assertThat(event.eventName).isEqualTo("samsung_pay.initialized")
        assertThat(event.params).isNull()
    }

    @Test
    fun `available event matches analytics contract`() {
        val event = OnrampAnalyticsEvent.SamsungPayAvailable(
            available = true,
            status = 2,
        )

        assertThat(event.eventName).isEqualTo("samsung_pay.available")
        assertThat(event.params).containsExactly(
            "available", true,
            "status", 2,
        )
    }

    @Test
    fun `presented event matches analytics contract`() {
        val event = OnrampAnalyticsEvent.SamsungPayPresented

        assertThat(event.eventName).isEqualTo("samsung_pay.presented")
        assertThat(event.params).isNull()
    }

    @Test
    fun `canceled event matches analytics contract`() {
        val event = OnrampAnalyticsEvent.SamsungPayCanceled

        assertThat(event.eventName).isEqualTo("samsung_pay.canceled")
        assertThat(event.params).isNull()
    }

    @Test
    fun `credential success event matches analytics contract`() {
        val event = OnrampAnalyticsEvent.SamsungPayObtainCredentialsSuccess

        assertThat(event.eventName).isEqualTo("samsung_pay.obtain_credentials.success")
        assertThat(event.params).isNull()
    }

    @Test
    fun `credential failure event matches analytics contract`() {
        val event = OnrampAnalyticsEvent.SamsungPayObtainCredentialsFailed(errorCode = -103)

        assertThat(event.eventName).isEqualTo("samsung_pay.obtain_credentials.failed")
        assertThat(event.params).containsExactly("error_code", -103)
    }
}
