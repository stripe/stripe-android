package com.stripe.android.paymentelement.embedded.content

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.Address
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ShippingDetailsInPaymentOptionPreview
import com.stripe.android.paymentsheet.PaymentOptionCardArtDrawableLoader
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@OptIn(AppearanceAPIAdditionsPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionDisplayDataFactoryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val displayDataFactory = createFactory()
    private val appearance = PaymentSheet.Appearance()

    @Test
    fun `create uses link account brand for saved Link passthrough card label`() {
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply {
            set(LinkAccountUpdate.Value(createLinkAccount(linkBrand = LinkBrand.Onelink)))
        }

        val option = createFactory(linkAccountHolder = linkAccountHolder).create(
            selection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(isLinkPassthroughMode = true),
            ),
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkBrand = LinkBrand.Link,
            ),
            appearance = appearance,
        )

        assertThat(option?.label).isEqualTo("Onelink")
    }

    @Test
    fun `create attaches PaymentMethod BillingDetails as PaymentSheet BillingDetails `() {
        val option = displayDataFactory.create(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION.copy(
                paymentMethodCreateParams = PaymentMethodCreateParams(
                    code = "card",
                    requiresMandate = false,
                    billingDetails = paymentMethodBillingDetails
                )
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        assertThat(option?.billingDetails).isEqualTo(paymentSheetBillingDetails)
    }

    @Test
    fun `create does not attach BillingDetails for Google Pay`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.GooglePay,
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        assertThat(option?.billingDetails).isNull()
    }

    @Test
    fun `selecting saved card does not attach mandate to paymentMethodMetadata`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        assertThat(option?.mandateText).isNull()
    }

    @Test
    fun `selecting new card does attach mandate to paymentMethodMetadata`() {
        val option = displayDataFactory.create(
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        assertThat(option?.mandateText).isNotNull()
    }

    @Test
    fun `selecting google pay does not attach mandate to paymentMethodMetadata`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.GooglePay,
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        assertThat(option?.mandateText).isNull()
    }

    @OptIn(ShippingDetailsInPaymentOptionPreview::class)
    @Test
    fun `create adds shipping details for verified Link user`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
                    collectedCvc = null,
                    billingPhone = null,
                ),
                shippingAddress = ConsumerShippingAddress(
                    id = "csmr_addr_123",
                    isDefault = true,
                    address = ConsumerPaymentDetails.BillingAddress(
                        name = "Jenny Rosen",
                        line1 = "123 Main St",
                        line2 = null,
                        locality = "San Francisco",
                        administrativeArea = "CA",
                        postalCode = "94111",
                        countryCode = CountryCode.US,
                    ),
                    unredactedPhoneNumber = "+15555555555",
                ),
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        assertThat(option?.shippingDetails).isEqualTo(
            AddressDetails(
                name = "Jenny Rosen",
                phoneNumber = "+15555555555",
                address = PaymentSheet.Address(
                    line1 = "123 Main St",
                    line2 = null,
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "US",
                ),
                isCheckboxSelected = null,
            )
        )
    }

    @OptIn(ShippingDetailsInPaymentOptionPreview::class)
    @Test
    fun `create adds no shipping details for unverified Link user`() {
        val option = displayDataFactory.create(
            selection = PaymentSelection.Link(
                brand = LinkBrand.Link,
                selectedPayment = null,
                shippingAddress = null,
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        assertThat(option?.shippingDetails).isNull()
    }

    @Test
    fun `always dark with dark component uses dark icon on light system`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysDark,
        lightComponent = Color.White,
        darkComponent = Color.Black,
        isSystemDarkTheme = false,
    ) {
        assertThat(loadedUrl).isEqualTo(DARK_ICON_URL)
    }

    @Test
    fun `always light with light component uses light icon on dark system`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.AlwaysLight,
        lightComponent = Color.White,
        darkComponent = Color.Black,
        isSystemDarkTheme = true,
    ) {
        assertThat(loadedUrl).isEqualTo(LIGHT_ICON_URL)
    }

    @Test
    fun `automatic with light component uses light icon on light system`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.Automatic,
        lightComponent = Color.White,
        darkComponent = Color.Black,
        isSystemDarkTheme = false,
    ) {
        assertThat(loadedUrl).isEqualTo(LIGHT_ICON_URL)
    }

    @Test
    fun `automatic with dark component uses dark icon on dark system`() = runIconScenario(
        themeMode = PaymentSheet.ThemeMode.Automatic,
        lightComponent = Color.White,
        darkComponent = Color.Black,
        isSystemDarkTheme = true,
    ) {
        assertThat(loadedUrl).isEqualTo(DARK_ICON_URL)
    }

    @Test
    fun `iconPainter uses card art when available`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cardArt = ColorDrawable()
        val cardArtLoadCalls = Turbine<PaymentSelection>()
        val imageLoader = FakeStripeImageLoader()
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val option = createFactory(
            iconLoader = PaymentSelection.IconLoader(
                resources = context.resources,
                imageLoader = imageLoader,
            ),
            cardArtDrawableLoader = { requestedSelection ->
                cardArtLoadCalls.add(requestedSelection)
                cardArt
            },
        ).create(
            selection = selection,
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
        )

        renderIcon(requireNotNull(option), isSystemDarkTheme = false)

        assertThat(cardArtLoadCalls.awaitItem()).isEqualTo(selection)
        cardArtLoadCalls.ensureAllEventsConsumed()
        imageLoader.ensureAllEventsConsumed()
    }

    private fun runIconScenario(
        themeMode: PaymentSheet.ThemeMode,
        lightComponent: Color,
        darkComponent: Color,
        isSystemDarkTheme: Boolean,
        block: IconScenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = FakeStripeImageLoader()
        val factory = createFactory(
            iconLoader = PaymentSelection.IconLoader(
                resources = context.resources,
                imageLoader = imageLoader,
            ),
        )
        val appearance = PaymentSheet.Appearance.Builder()
            .colorsLight(PaymentSheet.Colors.Builder.light().component(lightComponent).build())
            .colorsDark(PaymentSheet.Colors.Builder.dark().component(darkComponent).build())
            .themeMode(themeMode)
            .build()
        val selection = PaymentSelection.CustomPaymentMethod(
            id = "cpm_123",
            billingDetails = null,
            label = "CPM".resolvableString,
            lightThemeIconUrl = LIGHT_ICON_URL,
            darkThemeIconUrl = DARK_ICON_URL,
        )

        val option = requireNotNull(
            factory.create(
                selection = selection,
                paymentMethodMetadata = paymentMethodMetadata,
                appearance = appearance,
            )
        )
        renderIcon(option, isSystemDarkTheme)

        IconScenario(loadedUrl = imageLoader.awaitLoadCall().url).apply(block)
        imageLoader.ensureAllEventsConsumed()
    }

    private fun renderIcon(
        option: EmbeddedPaymentElement.PaymentOptionDisplayData,
        isSystemDarkTheme: Boolean,
    ) {
        composeRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                val nightMode = if (isSystemDarkTheme) {
                    Configuration.UI_MODE_NIGHT_YES
                } else {
                    Configuration.UI_MODE_NIGHT_NO
                }
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                option.iconPainter
            }
        }
        composeRule.waitForIdle()
    }

    companion object {
        private fun createFactory(
            iconLoader: PaymentSelection.IconLoader = mock(),
            cardArtDrawableLoader: PaymentOptionCardArtDrawableLoader = PaymentOptionCardArtDrawableLoader { null },
            linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        ) = PaymentOptionDisplayDataFactory(
            iconLoader = iconLoader,
            cardArtDrawableLoader = cardArtDrawableLoader,
            context = ApplicationProvider.getApplicationContext(),
            linkAccountHolder = linkAccountHolder,
        )

        private fun createLinkAccount(linkBrand: LinkBrand?): LinkAccount {
            return LinkAccount(
                TestFactory.CONSUMER_SESSION.copy(linkBrand = linkBrand),
            )
        }

        private val paymentSheetBillingDetails = PaymentSheet.BillingDetails(
            name = "Jenny Rosen",
            email = "foo@bar.com",
            phone = "+13105551234",
            address = PaymentSheet.Address(
                postalCode = "94111",
                country = "US",
            ),
        )
        private val paymentMethodBillingDetails = PaymentMethod.BillingDetails(
            address = Address(
                postalCode = "94111",
                country = "US",
            ),
            email = "foo@bar.com",
            name = "Jenny Rosen",
            phone = "+13105551234"
        )

        private val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_SUCCEEDED.copy(
                paymentMethodTypes = listOf("card", "cashapp", "google_pay"),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            allowsDelayedPaymentMethods = false,
            allowsPaymentMethodsRequiringShippingAddress = false,
            isGooglePayReady = true,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        )

        private const val LIGHT_ICON_URL = "light_icon_url"
        private const val DARK_ICON_URL = "dark_icon_url"
    }

    private data class IconScenario(
        val loadedUrl: String,
    )
}
