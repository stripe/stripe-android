package com.stripe.android.paymentelement

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CardFundingFilteringPrivatePreview
import com.stripe.android.paymentsheet.ConfigFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.TermsDisplay
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import org.junit.Test

class EmbeddedPaymentElementConfigurationTest {

    @OptIn(
        ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class,
        CardFundingFilteringPrivatePreview::class,
    )
    @Test
    fun `newBuilder round-trips all properties`() {
        val original = EmbeddedPaymentElement.Configuration.Builder("Test Merchant")
            .customer(PaymentSheet.CustomerConfiguration("cus_1", "ek_1"))
            .googlePay(ConfigFixtures.GOOGLE_PAY)
            .defaultBillingDetails(PaymentSheet.BillingDetails(name = "Jane"))
            .shippingDetails(
                AddressDetails(
                    name = "Jane",
                    address = PaymentSheet.Address(country = "US"),
                )
            )
            .allowsDelayedPaymentMethods(true)
            .allowsPaymentMethodsRequiringShippingAddress(true)
            .appearance(
                PaymentSheet.Appearance(
                    colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = Color.Red),
                )
            )
            .primaryButtonLabel("Pay")
            .billingDetailsCollectionConfiguration(
                BillingDetailsCollectionConfiguration(
                    name = CollectionMode.Always,
                )
            )
            .preferredNetworks(listOf(CardBrand.Visa))
            .allowsRemovalOfLastSavedPaymentMethod(false)
            .paymentMethodOrder(listOf("card"))
            .externalPaymentMethods(listOf("external_paypal"))
            .cardBrandAcceptance(
                PaymentSheet.CardBrandAcceptance.disallowed(
                    listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex),
                )
            )
            .allowedCardFundingTypes(listOf(PaymentSheet.CardFundingType.Credit))
            .customPaymentMethods(
                listOf(PaymentSheet.CustomPaymentMethod("cpmt_test", subtitle = null as String?))
            )
            .embeddedViewDisplaysMandateText(false)
            .link(
                PaymentSheet.LinkConfiguration(
                    display = PaymentSheet.LinkConfiguration.Display.Never,
                )
            )
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            .termsDisplay(mapOf(PaymentMethod.Type.Card to TermsDisplay.NEVER))
            .opensCardScannerAutomatically(true)
            .userOverrideCountry("GB")
            .build()

        val roundTripped = original.newBuilder().build()

        assertThat(roundTripped).isEqualTo(original)
    }

    @Test
    fun `Configuration property count matches expected - update newBuilder when this fails`() {
        val propertyCount = EmbeddedPaymentElement.Configuration::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .filterNot { it.name.startsWith("$") }
            .size
        // When a new property is added, this count will change, signaling that:
            // 1. newBuilder() needs to propagate the new property
            // 2. The round-trip test above needs a non-default value for it
            assertThat(propertyCount).isEqualTo(23)
    }
}
