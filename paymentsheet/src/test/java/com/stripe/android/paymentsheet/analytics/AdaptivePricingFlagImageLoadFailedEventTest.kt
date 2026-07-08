package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class AdaptivePricingFlagImageLoadFailedEventTest {

    private val testUrl =
        "https://img.stripecdn.com/cdn-cgi/image/format=auto,height=16,dpr=3/" +
            "https://b.stripecdn.com/ocs-mobile/assets/flags/EU.png"

    @Test
    fun `event name is correct`() {
        val event = PaymentSheetEvent.AdaptivePricingFlagImageLoadFailed(
            countryCode = "US",
            url = "https://example.com/flag.png",
        )

        assertThat(event.eventName)
            .isEqualTo("elements.adaptive_pricing.flag_image_load.failed")
    }

    @Test
    fun `params contain country_code and url`() {
        val event = PaymentSheetEvent.AdaptivePricingFlagImageLoadFailed(
            countryCode = "EU",
            url = testUrl,
        )

        assertThat(event.params).containsExactly(
            "country_code", "EU",
            "url", testUrl,
        )
    }
}
