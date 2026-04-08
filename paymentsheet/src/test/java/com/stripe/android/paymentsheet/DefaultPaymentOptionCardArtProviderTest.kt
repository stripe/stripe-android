package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.image.ImageOptimizer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultPaymentOptionCardArtProviderTest {

    @Test
    fun `returns optimized url for card art with art image`() {
        val provider = createProvider()

        val cardArt = PaymentMethod.Card.CardArt(
            artImage = PaymentMethod.Card.CardArt.ArtImage(
                format = "image/png",
                url = "https://example.com/card_art.png",
            ),
            programName = null,
        )

        val result = provider(cardArt)

        assertThat(result).isEqualTo("https://example.com/card_art.png?w=40")
    }

    @Test
    fun `returns null when art image is null`() {
        val provider = createProvider()

        val cardArt = PaymentMethod.Card.CardArt(
            artImage = null,
            programName = "My Card",
        )

        val result = provider(cardArt)

        assertThat(result).isNull()
    }

    private fun createProvider(): DefaultPaymentOptionCardArtProvider {
        return DefaultPaymentOptionCardArtProvider(
            context = ApplicationProvider.getApplicationContext(),
            imageOptimizer = FakeImageOptimizer(),
        )
    }

    private class FakeImageOptimizer : ImageOptimizer {
        override fun optimize(url: String, width: Int): String {
            return "$url?w=$width"
        }
    }
}
