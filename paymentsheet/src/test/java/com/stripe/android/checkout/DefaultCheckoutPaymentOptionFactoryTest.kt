package com.stripe.android.checkout

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeStripeImageLoader
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultCheckoutPaymentOptionFactoryTest {
    @Test
    fun `create returns null when there is no selection`() = runScenario {
        assertThat(factory.create(selection = null, paymentMethodMetadata = metadata)).isNull()
    }

    @Test
    fun `create maps a Google Pay selection`() = runScenario {
        val option = factory.create(selection = PaymentSelection.GooglePay, paymentMethodMetadata = metadata)

        assertThat(option).isNotNull()
        assertThat(option?.paymentMethodType).isEqualTo("google_pay")
        assertThat(option?.label).isEqualTo("Google Pay")
        assertThat(option?.mandateText).isNull()
    }

    @Test
    fun `create maps a new card selection`() = runScenario {
        val option = factory.create(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            paymentMethodMetadata = metadata,
        )

        assertThat(option).isNotNull()
        assertThat(option?.paymentMethodType).isEqualTo("card")
        assertThat(option?.label).contains("4242")
    }

    @Test
    fun `create attaches mandate text for a new card that requires setup`() = runScenario(
        // A SetupIntent forces the card form to require a mandate, unlike the default PaymentIntent metadata.
        metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_SUCCEEDED.copy(paymentMethodTypes = listOf("card")),
        ),
    ) {
        val option = factory.create(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.mandateText).isNotNull()
    }

    @Test
    fun `create does not attach mandate text for a saved card`() = runScenario {
        val option = factory.create(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        assertThat(option).isNotNull()
        assertThat(option?.paymentMethodType).isEqualTo("card")
        assertThat(option?.mandateText).isNull()
    }

    @Test
    fun `imageLoader returns the card art when the card art loader provides one`() = runScenario(
        cardArt = ColorDrawable(),
    ) {
        // Card art is only ever loaded for saved payment methods.
        val option = factory.create(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.imageLoader?.invoke()).isSameInstanceAs(cardArt)
    }

    @Test
    fun `imageLoader falls back to the icon loader when there is no card art`() = runScenario {
        val option = factory.create(
            selection = PaymentSelection.GooglePay,
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.imageLoader?.invoke()).isNotNull()
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        cardArt: Drawable? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Scenario(
            factory = DefaultCheckoutPaymentOptionDisplayDataFactory(
                iconLoader = PaymentSelection.IconLoader(
                    resources = context.resources,
                    imageLoader = FakeStripeImageLoader(),
                ),
                cardArtDrawableLoader = { cardArt },
                context = context,
                linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            ),
            metadata = metadata,
            cardArt = cardArt,
        ).block()
    }

    private class Scenario(
        val factory: DefaultCheckoutPaymentOptionDisplayDataFactory,
        val metadata: PaymentMethodMetadata,
        val cardArt: Drawable?,
    )
}
