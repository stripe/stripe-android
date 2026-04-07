package com.stripe.android.paymentsheet

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import org.junit.Test

class PaymentSheetConfigurationTest {

    @OptIn(
        ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class,
        WalletButtonsPreview::class,
        ShopPayPreview::class,
        CardFundingFilteringPrivatePreview::class,
        AddressAutocompletePreview::class,
    )
    @Test
    fun `newBuilder round-trips all properties`() {
        val original = PaymentSheet.Configuration(
            merchantDisplayName = "Test Merchant",
            customer = PaymentSheet.CustomerConfiguration("cus_1", "ek_1"),
            googlePay = ConfigFixtures.GOOGLE_PAY,
            defaultBillingDetails = PaymentSheet.BillingDetails(name = "Jane"),
            shippingDetails = AddressDetails(
                name = "Jane",
                address = PaymentSheet.Address(country = "US"),
            ),
            allowsDelayedPaymentMethods = true,
            allowsPaymentMethodsRequiringShippingAddress = true,
            appearance = PaymentSheet.Appearance(
                colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = Color.Red),
            ),
            colorScheme = PaymentSheet.ColorScheme.Dark,
            primaryButtonLabel = "Pay",
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                name = CollectionMode.Always,
            ),
            preferredNetworks = listOf(CardBrand.Visa),
            allowsRemovalOfLastSavedPaymentMethod = false,
            paymentMethodOrder = listOf("card"),
            externalPaymentMethods = listOf("external_paypal"),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex),
            ),
            allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Credit),
            customPaymentMethods = listOf(PaymentSheet.CustomPaymentMethod("cpmt_test", subtitle = null as String?)),
            link = PaymentSheet.LinkConfiguration(
                display = PaymentSheet.LinkConfiguration.Display.Never,
            ),
            walletButtons = PaymentSheet.WalletButtonsConfiguration(
                willDisplayExternally = true,
            ),
            shopPayConfiguration = PaymentSheet.ShopPayConfiguration(
                shopId = "shop_1",
                shippingAddressRequired = false,
                allowedShippingCountries = listOf("US"),
                lineItems = listOf(PaymentSheet.ShopPayConfiguration.LineItem("Item", 100)),
                shippingRates = emptyList(),
            ),
            googlePlacesApiKey = "test_api_key",
            termsDisplay = mapOf(PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.NEVER),
            opensCardScannerAutomatically = true,
            userOverrideCountry = "GB",
        )

        val roundTripped = original.newBuilder().build()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `Configuration property count matches expected - update newBuilder when this fails`() {
        val propertyCount = PaymentSheet.Configuration::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) } // Companion, CREATOR
            .filterNot { it.name.startsWith("$") } // Compose $stable
            .size
        // When a new property is added, this count will change, signaling that:
        // 1. newBuilder() needs to propagate the new property
        // 2. The round-trip test above needs a non-default value for it
        assertThat(propertyCount).isEqualTo(26)
    }
}
