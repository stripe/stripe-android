package com.stripe.android.paymentsheet.model

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentOptionCardArtDrawableLoader
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class PaymentOptionFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `create() with GooglePay should return expected object`() {
        val factory = createFactory()
        val paymentOption = factory.create(PaymentSelection.GooglePay, null, appearance = null)
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_google_pay_mark)
        assertThat(paymentOption.label).isEqualTo("Google Pay")
        assertThat(paymentOption.paymentMethodType).isEqualTo("google_pay")
        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with card PaymentMethod should return expected object`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                )
            ),
            null,
            appearance = null,
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with card params should return expected object`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.New.Card(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                ),
                brand = CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
            ),
            null,
            appearance = null,
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with card and Link inline signup should return card icon and label`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.New.Card(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                ),
                brand = CardBrand.Visa,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                linkInput = UserInput.SignUp(
                    email = "new_user@link.com",
                    phone = "+15555555555",
                    country = "US",
                    name = null,
                    consentAction = SignUpConsentAction.Checkbox,
                )
            ),
            null,
            appearance = null,
        )
        assertThat(paymentOption.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_card_visa_ref)
        assertThat(paymentOption.label).isEqualTo("···· 4242")
        assertThat(paymentOption.paymentMethodType).isEqualTo("card")
        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with saved card should include billing details when present`() {
        val factory = createFactory()
        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setBillingDetails(PAYMENT_METHOD_BILLING_DETAILS)
            .setCard(PaymentMethod.Card(last4 = "4242", brand = CardBrand.Visa, displayBrand = "visa"))
            .build()

        val paymentOption = factory.create(PaymentSelection.Saved(paymentMethod), null, appearance = null)

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with saved card should not include billing details when null`() {
        val factory = createFactory()
        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setCard(PaymentMethod.Card(last4 = "4242", brand = CardBrand.Visa, displayBrand = "visa"))
            .build()

        val paymentOption = factory.create(PaymentSelection.Saved(paymentMethod), null, appearance = null)

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with new generic payment method should include billing details when present`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.New.GenericPaymentMethod(
                iconResource = R.drawable.stripe_ic_paymentsheet_card_unknown_ref,
                iconResourceNight = null,
                label = "Test Payment Method".resolvableString,
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.PAYPAL.copy(
                    billingDetails = PAYMENT_METHOD_BILLING_DETAILS
                ),
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null
            ),
            null,
            appearance = null,
        )

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with Google Pay should not include billing details`() {
        val factory = createFactory()
        val paymentOption = factory.create(PaymentSelection.GooglePay, null, appearance = null)

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with Link should not include billing details`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.Link(brand = LinkBrand.Link),
            null,
            appearance = null,
        )

        assertThat(paymentOption.billingDetails).isNull()
    }

    @Test
    fun `create() with CPM should include billing details when present`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.CustomPaymentMethod(
                id = "cpm_123",
                billingDetails = PAYMENT_METHOD_BILLING_DETAILS,
                label = "CPM".resolvableString,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            ),
            null,
            appearance = null,
        )

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with EPM should include billing details when present`() {
        val factory = createFactory()
        val paymentOption = factory.create(
            PaymentSelection.ExternalPaymentMethod(
                type = "external_paypal",
                billingDetails = PAYMENT_METHOD_BILLING_DETAILS,
                label = "Paypal".resolvableString,
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
            ),
            null,
            appearance = null,
        )

        assertThat(paymentOption.billingDetails).isEqualTo(PAYMENT_SHEET_BILLING_DETAILS)
    }

    @Test
    fun `create() with partial billing details should map correctly`() {
        val factory = createFactory()
        val partialBillingDetails = PaymentMethod.BillingDetails(
            email = "test@example.com",
            name = "John Doe"
        )

        val paymentMethod = PaymentMethod.Builder()
            .setId("pm_1")
            .setCode("card")
            .setType(PaymentMethod.Type.Card)
            .setBillingDetails(partialBillingDetails)
            .setCard(PaymentMethod.Card(last4 = "4242", brand = CardBrand.Visa, displayBrand = "visa"))
            .build()

        val paymentOption = factory.create(PaymentSelection.Saved(paymentMethod), null, appearance = null)

        assertThat(paymentOption.billingDetails).isEqualTo(
            PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    city = null,
                    country = null,
                    line1 = null,
                    line2 = null,
                    postalCode = null,
                    state = null
                ),
                email = "test@example.com",
                name = "John Doe",
                phone = null
            )
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `always dark uses dark icon on light system`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysDark,
        lightComponent = Color.White,
        darkComponent = Color.Black,
    ) {
        assertThat(loadedUrl).isEqualTo(DARK_ICON_URL)
    }

    @Test
    @Config(qualifiers = "night")
    fun `always light uses light icon on dark system`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysLight,
        lightComponent = Color.White,
        darkComponent = Color.Black,
    ) {
        assertThat(loadedUrl).isEqualTo(LIGHT_ICON_URL)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `always dark with bright component uses light icon`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysDark,
        lightComponent = Color.Black,
        darkComponent = Color.White,
    ) {
        assertThat(loadedUrl).isEqualTo(LIGHT_ICON_URL)
    }

    @Test
    @Config(qualifiers = "night")
    fun `always light with dark component uses dark icon`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysLight,
        lightComponent = Color.Black,
        darkComponent = Color.White,
    ) {
        assertThat(loadedUrl).isEqualTo(DARK_ICON_URL)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `missing appearance uses light icon on light system`() = runIconScenario(
        appearance = null,
    ) {
        assertThat(loadedUrl).isEqualTo(LIGHT_ICON_URL)
    }

    @Test
    @Config(qualifiers = "night")
    fun `missing appearance uses dark icon on dark system`() = runIconScenario(
        appearance = null,
    ) {
        assertThat(loadedUrl).isEqualTo(DARK_ICON_URL)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `missing appearance uses dark icon for legacy custom dark theme`() {
        val originalColors = StripeTheme.colorsLightMutable
        try {
            StripeTheme.colorsLightMutable = originalColors.copy(component = Color.Black)

            runIconScenario(appearance = null) {
                assertThat(loadedUrl).isEqualTo(DARK_ICON_URL)
            }
        } finally {
            StripeTheme.colorsLightMutable = originalColors
        }
    }

    @Test
    fun `icon() returns card art drawable when loader provides one`() {
        val cardArtDrawable = ShapeDrawable()
        val factory = createFactory(
            cardArtDrawableLoader = { cardArtDrawable },
        )

        val option = factory.create(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            null,
            appearance = null,
        )
        val icon = option.icon()

        assertThat(icon.current).isEqualTo(cardArtDrawable)
    }

    @Test
    fun `icon() falls back to icon loader when card art loader returns null`() {
        val factory = createFactory(
            cardArtDrawableLoader = { null },
        )

        val option = factory.create(
            PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            null,
            appearance = null,
        )
        val icon = option.icon()

        assertThat(icon.current).isNotInstanceOf(ShapeDrawable::class.java)
    }

    private fun runIconScenario(
        themeMode: PaymentSheet.ThemeMode,
        lightComponent: Color,
        darkComponent: Color,
        block: IconScenario.() -> Unit,
    ) = runIconScenario(
        appearance = PaymentSheet.Appearance.Builder()
            .colorsLight(PaymentSheet.Colors.Builder.light().component(lightComponent).build())
            .colorsDark(PaymentSheet.Colors.Builder.dark().component(darkComponent).build())
            .themeMode(themeMode)
            .build(),
        block = block,
    )

    private fun runIconScenario(
        appearance: PaymentSheet.Appearance?,
        block: IconScenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = FakeStripeImageLoader()
        val factory = PaymentOptionFactory(
            iconLoader = PaymentSelection.IconLoader(
                resources = context.resources,
                imageLoader = imageLoader,
            ),
            cardArtDrawableLoader = { null },
            context = context,
        )
        val selection = PaymentSelection.CustomPaymentMethod(
            id = "cpm_123",
            billingDetails = null,
            label = "CPM".resolvableString,
            lightThemeIconUrl = LIGHT_ICON_URL,
            darkThemeIconUrl = DARK_ICON_URL,
        )

        factory.create(
            selection = selection,
            linkBrand = null,
            appearance = appearance,
        ).icon()

        IconScenario(loadedUrl = imageLoader.awaitLoadCall().url).apply(block)
        imageLoader.ensureAllEventsConsumed()
    }

    private fun createFactory(
        cardArtDrawableLoader: PaymentOptionCardArtDrawableLoader = PaymentOptionCardArtDrawableLoader { null },
    ): PaymentOptionFactory {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return PaymentOptionFactory(
            iconLoader = PaymentSelection.IconLoader(
                resources = context.resources,
                imageLoader = DefaultStripeImageLoader(context),
            ),
            cardArtDrawableLoader = cardArtDrawableLoader,
            context = context,
        )
    }

    private companion object {
        const val LIGHT_ICON_URL = "light_icon_url"
        const val DARK_ICON_URL = "dark_icon_url"

        val PAYMENT_METHOD_BILLING_DETAILS = PaymentMethod.BillingDetails(
            address = Address(
                city = "San Francisco",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4B",
                postalCode = "94102",
                state = "CA"
            ),
            email = "test@example.com",
            name = "John Doe",
            phone = "+15555555555"
        )

        val PAYMENT_SHEET_BILLING_DETAILS = PaymentSheet.BillingDetails(
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "123 Main St",
                line2 = "Apt 4B",
                postalCode = "94102",
                state = "CA"
            ),
            email = "test@example.com",
            name = "John Doe",
            phone = "+15555555555"
        )
    }

    private data class IconScenario(
        val loadedUrl: String,
    )
}
