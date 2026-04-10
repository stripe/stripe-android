package com.stripe.android.paymentsheet

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeStripeImageLoader
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultPaymentOptionCardArtDrawableLoaderTest {

    @Test
    fun `load returns drawable for saved card with card art`() = runScenario(
        cardArtProviderResult = CARD_ART_URL,
        getResult = Result.success(TEST_BITMAP),
    ) {
        val result = loader.load(savedCardWithCardArt())

        assertThat(fakeImageLoader.awaitGetCall()).isEqualTo(CARD_ART_URL)
        assertThat(result).isInstanceOf(BitmapDrawable::class.java)
    }

    @Test
    fun `load returns null for non-saved selection`() = runScenario {
        val result = loader.load(PaymentSelection.GooglePay)

        assertThat(result).isNull()
    }

    @Test
    fun `load returns null for saved card without card art`() = runScenario {
        val result = loader.load(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        assertThat(result).isNull()
    }

    @Test
    fun `load returns null when provider returns null`() = runScenario(
        cardArtProviderResult = null,
    ) {
        val result = loader.load(savedCardWithCardArt())

        assertThat(result).isNull()
    }

    @Test
    fun `load returns null and reports error on failure`() = runScenario(
        cardArtProviderResult = CARD_ART_URL,
        getResult = Result.failure(RuntimeException("load failed")),
    ) {
        val result = loader.load(savedCardWithCardArt())

        assertThat(fakeImageLoader.awaitGetCall()).isEqualTo(CARD_ART_URL)
        assertThat(result).isNull()

        val errorCall = fakeErrorReporter.awaitCall()
        assertThat(errorCall.errorEvent).isEqualTo(ExpectedErrorEvent.PAYMENT_OPTION_CARD_ART_LOAD_FAILURE)
    }

    @Test
    fun `load returns null when image loader returns null bitmap`() = runScenario(
        cardArtProviderResult = CARD_ART_URL,
        getResult = Result.success(null),
    ) {
        val result = loader.load(savedCardWithCardArt())

        assertThat(fakeImageLoader.awaitGetCall()).isEqualTo(CARD_ART_URL)
        assertThat(result).isNull()
    }

    private fun savedCardWithCardArt(): PaymentSelection.Saved {
        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setCard(
                PaymentMethod.Card(
                    last4 = "4242",
                    brand = CardBrand.Visa,
                    displayBrand = "visa",
                    cardArt = PaymentMethod.Card.CardArt(
                        artImage = PaymentMethod.Card.CardArt.ArtImage(
                            format = "image/png",
                            url = "https://example.com/card_art.png",
                        ),
                        programName = null,
                    ),
                )
            )
            .build()
        return PaymentSelection.Saved(paymentMethod)
    }

    private fun runScenario(
        cardArtProviderResult: String? = null,
        getResult: Result<Bitmap?> = Result.success(null),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeImageLoader = FakeStripeImageLoader(getResult = getResult)
        val fakeErrorReporter = FakeErrorReporter()

        val loader = DefaultPaymentOptionCardArtDrawableLoader(
            paymentOptionCardArtProvider = { cardArtProviderResult },
            imageLoader = fakeImageLoader,
            errorReporter = fakeErrorReporter,
            context = ApplicationProvider.getApplicationContext(),
        )

        Scenario(
            loader = loader,
            fakeImageLoader = fakeImageLoader,
            fakeErrorReporter = fakeErrorReporter,
        ).apply { block() }

        fakeImageLoader.ensureAllEventsConsumed()
        fakeErrorReporter.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val loader: DefaultPaymentOptionCardArtDrawableLoader,
        val fakeImageLoader: FakeStripeImageLoader,
        val fakeErrorReporter: FakeErrorReporter,
    )

    private companion object {
        const val CARD_ART_URL = "https://example.com/optimized.png"
        val TEST_BITMAP: Bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    }
}
