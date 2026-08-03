package com.stripe.android.common.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import org.junit.Test

internal class CommonConfigurationTest {
    private val configuration = CommonConfigurationFactory.create()

    @Test
    fun `'containVolatileDifferences' should return false when no volatile differences are found`() {
        val changedConfiguration = configuration.copy(
            merchantDisplayName = "New merchant, Inc.",
        )

        assertThat(configuration.containsVolatileDifferences(changedConfiguration)).isFalse()
    }

    @Test
    fun `'containVolatileDifferences' should return true when volatile differences are found`() {
        val configWithCardBrandAcceptanceChanges = configuration.copy(
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
            )
        )

        assertThat(configuration.containsVolatileDifferences(configWithCardBrandAcceptanceChanges)).isTrue()

        val configWithBillingDetailsChanges = configuration.copy(
            defaultBillingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Richards",
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingDetailsChanges)).isTrue()

        val configWithBillingConfigChanges = configuration.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingConfigChanges)).isTrue()
    }

    @Test
    fun `allowedCardFundingTypes returns configured list when enabled is true`() {
        val customFundingTypes = listOf(PaymentSheet.CardFundingType.Credit)
        val config = configuration.copy(
            allowedCardFundingTypes = customFundingTypes
        )

        assertThat(config.allowedCardFundingTypes(enabled = true))
            .isEqualTo(customFundingTypes)
    }

    @Test
    fun `allowedCardFundingTypes returns default list when enabled is false`() {
        val customFundingTypes = listOf(PaymentSheet.CardFundingType.Credit)
        val config = configuration.copy(
            allowedCardFundingTypes = customFundingTypes
        )

        assertThat(config.allowedCardFundingTypes(enabled = false))
            .isEqualTo(PaymentSheet.CardFundingType.entries)
    }

    @Test
    @Suppress("LongMethod")
    fun `toMobileSessionConfig maps privacy minimized configuration capabilities`() {
        val configuration = CommonConfigurationFactory.create(
            merchantDisplayName = "Merchant",
            customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession("cus_123", "cuss_123_secret"),
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "GB",
                currencyCode = "GBP",
                amount = 1099,
                label = "Total",
                buttonType = PaymentSheet.GooglePayConfiguration.ButtonType.Checkout,
                additionalEnabledNetworks = listOf("INTERAC"),
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    city = "London",
                    country = "GB",
                    line1 = "1 Main St",
                    line2 = "Flat 2",
                    postalCode = "SW1A 1AA",
                    state = "London",
                ),
                email = "jenny@example.com",
                name = "Jenny",
                phone = "5551234567",
            ),
            shippingDetails = AddressDetails(name = "Jenny"),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = true,
                allowedCountries = setOf("US", "GB"),
            ),
            preferredNetworks = listOf(CardBrand.Visa),
            allowsRemovalOfLastSavedPaymentMethod = false,
            paymentMethodOrder = listOf("card", "link"),
            externalPaymentMethods = listOf("external_paypal"),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            customPaymentMethods = listOf(
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_123",
                    subtitle = "Pay another way",
                    disableBillingDetailCollection = false,
                )
            ),
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.allowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
            ),
            link = PaymentSheet.LinkConfiguration(
                display = PaymentSheet.LinkConfiguration.Display.Never,
                collectMissingBillingDetailsForExistingPaymentMethods = false,
                allowUserEmailEdits = false,
                allowLogOut = false,
                disallowFundingSourceCreation = setOf("card"),
            ),
            googlePlacesApiKey = "places_key",
            termsDisplay = mapOf(PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.NEVER),
            walletButtons = PaymentSheet.WalletButtonsConfiguration(
                willDisplayExternally = true,
                visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                    paymentElement = mapOf(
                        PaymentSheet.WalletButtonsConfiguration.Wallet.Link to
                            PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility.Never
                    ),
                    walletButtonsView = mapOf(
                        PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay to
                            PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility.Always
                    ),
                ),
            ),
            opensCardScannerAutomatically = true,
            userOverrideCountry = "CA",
            appearance = PaymentSheet.Appearance.Builder()
                .themeMode(PaymentSheet.ThemeMode.AlwaysDark)
                .build(),
            allowedCardFundingTypes = listOf(PaymentSheet.CardFundingType.Credit),
        )

        val mapped = configuration.toMobileSessionConfig()

        assertThat(mapped.googlePay?.merchantCountryCode).isEqualTo("GB")
        assertThat(mapped.googlePay?.environment).isEqualTo("test")
        assertThat(mapped.googlePay?.amountProvided).isTrue()
        assertThat(mapped.googlePay?.labelProvided).isTrue()
        assertThat(mapped.googlePay?.buttonType).isEqualTo("checkout")
        assertThat(mapped.link.disabledFundingSources).containsExactly("card")
        assertThat(mapped.link.collectMissingBillingDetailsForExistingPaymentMethods).isFalse()
        assertThat(mapped.customerConfigured).isTrue()
        assertThat(mapped.customerAccessType).isEqualTo("customer_session")
        assertThat(mapped.defaultBillingDetails.addressCountryCode).isEqualTo("GB")
        assertThat(mapped.defaultBillingDetails.addressLine2).isTrue()
        assertThat(mapped.shippingDetailsProvided).isTrue()
        assertThat(mapped.appearanceCustomized).isTrue()
        assertThat(mapped.preferredNetworks).containsExactly("visa")
        assertThat(mapped.billingDetailsCollectionConfiguration.allowedCountries)
            .containsExactly("GB", "US").inOrder()
        assertThat(mapped.externalPaymentMethods).containsExactly("external_paypal")
        assertThat(mapped.customPaymentMethods.single().subtitleProvided).isTrue()
        assertThat(mapped.customPaymentMethods.single().disableBillingDetailCollection).isFalse()
        assertThat(mapped.paymentMethodOrder).containsExactly("card", "link").inOrder()
        assertThat(mapped.paymentMethodLayout).isEqualTo("vertical")
        assertThat(mapped.cardBrandAcceptance.brands).containsExactly("visa")
        assertThat(mapped.allowedCardFundingTypes).containsExactly("credit")
        assertThat(mapped.termsDisplay).containsExactly("card", "never")
        assertThat(mapped.allowsRemovalOfLastSavedPaymentMethod).isFalse()
        assertThat(mapped.opensCardScannerAutomatically).isTrue()
        assertThat(mapped.walletButtons.willDisplayExternally).isTrue()
        assertThat(mapped.walletButtons.paymentElement).containsExactly("link", "never")
        assertThat(mapped.walletButtons.walletButtonsView).containsExactly("google_pay", "always")
        assertThat(mapped.googlePlacesApiKeyProvided).isTrue()
        assertThat(mapped.userOverrideCountry).isEqualTo("CA")
    }
}
