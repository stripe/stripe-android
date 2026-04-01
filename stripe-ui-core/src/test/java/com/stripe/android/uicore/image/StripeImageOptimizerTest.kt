package com.stripe.android.uicore.image

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class StripeImageOptimizerTest {

    @Test
    fun `optimize returns Stripe CDN URL with height and dpr`() {
        val result = StripeImageOptimizer.optimize(
            url = "https://example.com/card_art.png",
            width = 120
        )

        assertThat(result).isEqualTo(
            "https://img.stripecdn.com/cdn-cgi/image/format=auto,width=120,dpr=3/https://example.com/card_art.png"
        )
    }
}
