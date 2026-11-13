package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import org.json.JSONObject
import org.junit.Test

internal class MobileSessionConfigTest {
    @Test
    fun `default configuration encodes explicit Android capabilities and collection defaults`() {
        val config = DEFAULT_CONFIGURATION.toMobileSessionConfig(hasFinancialConnectionsSdk = false)
        val json = JSONObject(MobileSession.encode(config))
        val paymentElement = json.getJSONObject("payment_element_config")
        val billingCollection = paymentElement.getJSONObject("billing_details_collection")

        assertThat(json.getString("spec_hash")).isEqualTo("f718567f")
        assertThat(paymentElement.getBoolean("allows_delayed_payment_methods")).isFalse()
        assertThat(paymentElement.getBoolean("has_return_url")).isTrue()
        assertThat(paymentElement.getJSONObject("default_billing_details").getBoolean("email")).isFalse()
        assertThat(billingCollection.getString("name")).isEqualTo("automatic")
        assertThat(billingCollection.getString("phone")).isEqualTo("automatic")
        assertThat(billingCollection.getString("email")).isEqualTo("automatic")
        assertThat(billingCollection.getString("address")).isEqualTo("automatic")
        assertThat(billingCollection.getBoolean("attach_defaults_to_payment_method")).isFalse()
        assertThat(paymentElement.getJSONObject("link").getString("display_mode")).isEqualTo("automatic")
        assertThat(paymentElement.getJSONObject("card_brand_acceptance").getString("filter")).isEqualTo("all")
        assertThat(paymentElement.getJSONObject("card_brand_acceptance").getJSONArray("brands").length()).isEqualTo(0)
        assertThat(paymentElement.getJSONArray("payment_method_order").length()).isEqualTo(0)
        assertThat(json.getJSONObject("environment").getBoolean("has_financial_connections_sdk")).isFalse()
        assertThat(json.getJSONObject("environment").getBoolean("device_supports_native_link")).isTrue()
    }

    @Test
    fun `populated configuration maps settings without sending billing values`() {
        val configuration = DEFAULT_CONFIGURATION.copy(
            allowsDelayedPaymentMethods = true,
            defaultBillingDetails = PaymentSheet.BillingDetails(email = " jane@example.com ", name = "Jane"),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = true,
            ),
            link = PaymentSheet.LinkConfiguration(PaymentSheet.LinkConfiguration.Display.WalletButtonHidden),
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Amex),
            ),
            paymentMethodOrder = listOf("card", "external_paypal", "cpmt_custom"),
        )
        val config = configuration.toMobileSessionConfig(hasFinancialConnectionsSdk = true)
        val header = MobileSession.encode(config)
        val paymentElement = config.paymentElementConfig

        assertThat(paymentElement.allowsDelayedPaymentMethods).isTrue()
        assertThat(paymentElement.defaultBillingDetails.email).isTrue()
        assertThat(paymentElement.billingDetailsCollection.name).isEqualTo(MobileSession.CollectionMode.ALWAYS)
        assertThat(paymentElement.billingDetailsCollection.phone).isEqualTo(MobileSession.CollectionMode.NEVER)
        assertThat(paymentElement.billingDetailsCollection.email).isEqualTo(MobileSession.CollectionMode.ALWAYS)
        assertThat(paymentElement.billingDetailsCollection.address).isEqualTo(MobileSession.AddressCollectionMode.FULL)
        assertThat(paymentElement.billingDetailsCollection.attachDefaultsToPaymentMethod).isTrue()
        assertThat(paymentElement.link.displayMode).isEqualTo(MobileSession.LinkDisplay.WALLET_BUTTON_HIDDEN)
        assertThat(paymentElement.cardBrandAcceptance.filter).isEqualTo(MobileSession.CardBrandFilter.DISALLOWED)
        assertThat(paymentElement.cardBrandAcceptance.brands).containsExactly(MobileSession.CardBrandCategory.AMEX)
        assertThat(paymentElement.paymentMethodOrder)
            .containsExactly("card", "external_paypal", "cpmt_custom").inOrder()
        assertThat(config.environment.hasFinancialConnectionsSdk).isTrue()
        assertThat(header).doesNotContain("jane@example.com")
        assertThat(header).doesNotContain("Jane")
    }

    @Test
    fun `blank default email is absent`() {
        val config = DEFAULT_CONFIGURATION.copy(
            defaultBillingDetails = PaymentSheet.BillingDetails(email = " \t\n "),
        ).toMobileSessionConfig(hasFinancialConnectionsSdk = false)

        assertThat(config.paymentElementConfig.defaultBillingDetails.email).isFalse()
    }

    @Test
    fun `disabled Link and address collection map to never`() {
        val config = DEFAULT_CONFIGURATION.copy(
            link = PaymentSheet.LinkConfiguration(PaymentSheet.LinkConfiguration.Display.Never),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
        ).toMobileSessionConfig(hasFinancialConnectionsSdk = false)

        assertThat(config.paymentElementConfig.link.displayMode).isEqualTo(MobileSession.LinkDisplay.NEVER)
        assertThat(config.paymentElementConfig.billingDetailsCollection.address)
            .isEqualTo(MobileSession.AddressCollectionMode.NEVER)
    }

    @Test
    fun `allowed card brands preserve all categories`() {
        val config = DEFAULT_CONFIGURATION.copy(
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.allowed(
                PaymentSheet.CardBrandAcceptance.BrandCategory.entries,
            ),
        ).toMobileSessionConfig(hasFinancialConnectionsSdk = false)

        assertThat(config.paymentElementConfig.cardBrandAcceptance.filter)
            .isEqualTo(MobileSession.CardBrandFilter.ALLOWED)
        assertThat(config.paymentElementConfig.cardBrandAcceptance.brands).containsExactly(
            MobileSession.CardBrandCategory.VISA,
            MobileSession.CardBrandCategory.MASTERCARD,
            MobileSession.CardBrandCategory.AMEX,
            MobileSession.CardBrandCategory.DISCOVER,
        ).inOrder()
    }

    @Test
    fun `header escapes Unicode payment method names and preserves their order`() {
        val config = DEFAULT_CONFIGURATION.copy(
            paymentMethodOrder = listOf("café", "😀"),
        ).toMobileSessionConfig(hasFinancialConnectionsSdk = false)

        val header = MobileSession.encode(config)

        assertThat(header.all { it.code < 128 }).isTrue()
        assertThat(header).contains("caf\\u00e9")
        assertThat(header).contains("\\ud83d\\ude00")
        val order = JSONObject(header).getJSONObject("payment_element_config").getJSONArray("payment_method_order")
        assertThat(order.getString(0)).isEqualTo("café")
        assertThat(order.getString(1)).isEqualTo("😀")
    }

    private companion object {
        val DEFAULT_CONFIGURATION = PaymentSheet.Configuration("Merchant").asCommonConfiguration()
    }
}
