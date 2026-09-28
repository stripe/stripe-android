package com.stripe.android.common.taptoadd

import com.google.common.truth.Truth.assertThat

class TapToAddCardArtTestHelper(
    private val imageLoaderTestRule: TapToAddStripeImageLoaderTestRule,
) {
    suspend fun assertCardArtAssetPreloads() {
        /*
         * These images are loaded asynchronously all together. The order itself does not matter, only that all the
         * images were asked to be loaded.
         */
        val preloadingImages = setOf(
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
        )

        assertThat(preloadingImages).containsExactly(
            "https://b.stripecdn.com/ocs-mobile/assets/visa.png",
            "https://b.stripecdn.com/ocs-mobile/assets/mastercard.png",
            "https://b.stripecdn.com/ocs-mobile/assets/discover.webp",
            "https://b.stripecdn.com/ocs-mobile/assets/amex.webp",
            "https://b.stripecdn.com/ocs-mobile/assets/jcb.png"
        )
    }
}
