package com.stripe.android.checkout

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
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
    fun `create populates billing details from a saved payment method`() = runScenario {
        val option = factory.create(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        val billingDetails = requireNotNull(option?.billingDetails)
        assertThat(billingDetails.name).isEqualTo("Jenny Rosen")
        assertThat(billingDetails.email).isEqualTo("jenny.rosen@example.com")
        assertThat(billingDetails.address?.line1).isEqualTo("1234 Main Street")
        assertThat(billingDetails.address?.city).isEqualTo("San Francisco")
        assertThat(billingDetails.address?.state).isEqualTo("CA")
        assertThat(billingDetails.address?.postalCode).isEqualTo("94111")
        assertThat(billingDetails.address?.country).isEqualTo("US")
    }

    @Test
    fun `create leaves billing details null for Google Pay`() = runScenario {
        val option = factory.create(selection = PaymentSelection.GooglePay, paymentMethodMetadata = metadata)

        assertThat(option?.billingDetails).isNull()
    }

    @Test
    fun `create leaves address null when the payment method has billing details but no address`() = runScenario {
        val option = factory.create(
            selection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                    billingDetails = PaymentMethod.BillingDetails(
                        name = "Jenny Rosen",
                        email = "jenny.rosen@example.com",
                        address = null,
                    ),
                ),
            ),
            paymentMethodMetadata = metadata,
        )

        val billingDetails = requireNotNull(option?.billingDetails)
        assertThat(billingDetails.name).isEqualTo("Jenny Rosen")
        assertThat(billingDetails.email).isEqualTo("jenny.rosen@example.com")
        assertThat(billingDetails.address).isNull()
    }

    @Test
    fun `paymentOptionResource returns the card art when the card art loader provides one`() = runScenario(
        cardArt = ColorDrawable(),
    ) {
        // Card art is only ever loaded for saved payment methods.
        val option = factory.create(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.paymentOptionResource?.load(isSystemDarkTheme = false)).isSameInstanceAs(cardArt)
    }

    @Test
    fun `paymentOptionResource falls back to the icon loader when there is no card art`() = runScenario {
        val option = factory.create(
            selection = PaymentSelection.GooglePay,
            paymentMethodMetadata = metadata,
        )

        assertThat(option?.paymentOptionResource?.load(isSystemDarkTheme = false)).isNotNull()
    }

    @Test
    fun `paymentOptionResource uses light icon on light system`() = runScenario {
        val option = factory.create(
            selection = customPaymentMethod,
            paymentMethodMetadata = metadata,
        )

        option?.paymentOptionResource?.load(isSystemDarkTheme = false)

        assertThat(imageLoader.awaitLoadCall().url).isEqualTo(LIGHT_ICON_URL)
    }

    @Test
    fun `paymentOptionResource uses dark icon on dark system`() = runScenario {
        val option = factory.create(
            selection = customPaymentMethod,
            paymentMethodMetadata = metadata,
        )

        option?.paymentOptionResource?.load(isSystemDarkTheme = true)

        assertThat(imageLoader.awaitLoadCall().url).isEqualTo(DARK_ICON_URL)
    }

    @Test
    fun `create uses link account brand for saved Link passthrough card label`() = runScenario(
        linkAccount = LinkAccount(
            TestFactory.CONSUMER_SESSION.copy(linkBrand = LinkBrand.Onelink),
        )
    ) {
        val option = factory.create(
            selection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(isLinkPassthroughMode = true),
            ),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkBrand = LinkBrand.Link,
            ),
        )

        assertThat(option?.label).isEqualTo("Onelink")
    }

    private fun runScenario(
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        cardArt: Drawable? = null,
        linkAccount: LinkAccount? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = FakeStripeImageLoader()
        Scenario(
            factory = DefaultCheckoutPaymentOptionDisplayDataFactory(
                iconLoader = PaymentSelection.IconLoader(
                    resources = context.resources,
                    imageLoader = imageLoader,
                ),
                cardArtDrawableLoader = { cardArt },
                context = context,
                linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply {
                    set(LinkAccountUpdate.Value(linkAccount))
                },
            ),
            metadata = metadata,
            cardArt = cardArt,
            imageLoader = imageLoader,
        ).apply { block() }
        imageLoader.ensureAllEventsConsumed()
    }

    private class Scenario(
        val factory: DefaultCheckoutPaymentOptionDisplayDataFactory,
        val metadata: PaymentMethodMetadata,
        val cardArt: Drawable?,
        val imageLoader: FakeStripeImageLoader,
    )

    private companion object {
        const val LIGHT_ICON_URL = "light_icon_url"
        const val DARK_ICON_URL = "dark_icon_url"

        val customPaymentMethod = PaymentSelection.CustomPaymentMethod(
            id = "cpm_123",
            billingDetails = null,
            label = "Custom".resolvableString,
            lightThemeIconUrl = LIGHT_ICON_URL,
            darkThemeIconUrl = DARK_ICON_URL,
        )
    }
}
