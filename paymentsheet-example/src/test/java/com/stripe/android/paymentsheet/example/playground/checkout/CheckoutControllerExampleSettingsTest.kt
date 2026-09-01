package com.stripe.android.paymentsheet.example.playground.checkout

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CheckoutControllerExampleSettingsTest {
    @Test
    fun `settings list roots in order and adds active children in order`() {
        val defaultSettings = CheckoutControllerExampleSettings.create(
            persistedValues = null,
            storedCustomerId = null,
        )
        val automaticTaxSettings = defaultSettings.withValue(
            CheckoutControllerExampleSettingsDefinition.AutomaticTax,
            true,
        )

        assertThat(defaultSettings.activeSettings().map { it.definition.key }).containsExactly(
            CheckoutControllerExampleSettingKey.Customer,
            CheckoutControllerExampleSettingKey.AutomaticTax,
        ).inOrder()
        assertThat(automaticTaxSettings.activeSettings().map { it.definition.key }).containsExactly(
            CheckoutControllerExampleSettingKey.Customer,
            CheckoutControllerExampleSettingKey.AutomaticTax,
            CheckoutControllerExampleSettingKey.ShippingAddressCollection,
            CheckoutControllerExampleSettingKey.BillingAddressCollection,
        ).inOrder()
    }

    @Test
    fun `fresh settings select and offer an existing customer when one is stored`() {
        val settings = CheckoutControllerExampleSettings.create(
            persistedValues = null,
            storedCustomerId = "cus_123",
        )

        assertThat(settings[CheckoutControllerExampleSettingsDefinition.Customer]).isEqualTo(
            CheckoutControllerExampleCustomer.Existing("cus_123")
        )
        assertThat(
            CheckoutControllerExampleSettingsDefinition.Customer.options(settings).map { it.value }
        ).containsExactly(
            CheckoutControllerExampleCustomer.Guest,
            CheckoutControllerExampleCustomer.New,
            CheckoutControllerExampleCustomer.Existing("cus_123"),
        ).inOrder()
    }

    @Test
    fun `fresh settings omit the existing customer option when no ID is stored`() {
        val settings = CheckoutControllerExampleSettings.create(
            persistedValues = null,
            storedCustomerId = null,
        )

        assertThat(settings[CheckoutControllerExampleSettingsDefinition.Customer]).isEqualTo(
            CheckoutControllerExampleCustomer.Guest
        )
        assertThat(
            CheckoutControllerExampleSettingsDefinition.Customer.options(settings).map { it.value }
        ).containsExactly(
            CheckoutControllerExampleCustomer.Guest,
            CheckoutControllerExampleCustomer.New,
        ).inOrder()
    }

    @Test
    fun `settings encode and restore their typed values`() {
        val settings = CheckoutControllerExampleSettings.create(
            persistedValues = null,
            storedCustomerId = "cus_123",
        )
            .withValue(
                CheckoutControllerExampleSettingsDefinition.Customer,
                CheckoutControllerExampleCustomer.Existing("cus_456"),
            )
            .withValue(CheckoutControllerExampleSettingsDefinition.AutomaticTax, true)
            .withValue(CheckoutControllerExampleSettingsDefinition.ShippingAddressCollection, false)
            .withValue(CheckoutControllerExampleSettingsDefinition.BillingAddressCollection, true)

        assertThat(settings.encodedValues()).isEqualTo(
            mapOf(
                "customer" to "existing:cus_456",
                "automatic_tax" to "true",
                "shipping_address_collection" to "false",
                "billing_address_collection" to "true",
            )
        )

        val restoredSettings = CheckoutControllerExampleSettings.create(
            persistedValues = settings.encodedValues(),
            storedCustomerId = "cus_456",
        )

        assertThat(restoredSettings[CheckoutControllerExampleSettingsDefinition.Customer]).isEqualTo(
            CheckoutControllerExampleCustomer.Existing("cus_456")
        )
        assertThat(restoredSettings[CheckoutControllerExampleSettingsDefinition.AutomaticTax]).isTrue()
        assertThat(restoredSettings[CheckoutControllerExampleSettingsDefinition.ShippingAddressCollection]).isFalse()
        assertThat(restoredSettings[CheckoutControllerExampleSettingsDefinition.BillingAddressCollection]).isTrue()
    }

    @Test
    fun `automatic tax retains hidden address collection values`() {
        val settingsWithAutomaticTax = CheckoutControllerExampleSettings.create(
            persistedValues = null,
            storedCustomerId = null,
        )
            .withValue(CheckoutControllerExampleSettingsDefinition.AutomaticTax, true)
            .withValue(CheckoutControllerExampleSettingsDefinition.ShippingAddressCollection, false)
            .withValue(CheckoutControllerExampleSettingsDefinition.BillingAddressCollection, true)
        val settingsWithoutAutomaticTax = settingsWithAutomaticTax.withValue(
            CheckoutControllerExampleSettingsDefinition.AutomaticTax,
            false,
        )
        val restoredAutomaticTaxSettings = settingsWithoutAutomaticTax.withValue(
            CheckoutControllerExampleSettingsDefinition.AutomaticTax,
            true,
        )

        assertThat(settingsWithoutAutomaticTax.activeSettings().map { it.definition.key }).containsExactly(
            CheckoutControllerExampleSettingKey.Customer,
            CheckoutControllerExampleSettingKey.AutomaticTax,
        ).inOrder()
        assertThat(settingsWithoutAutomaticTax[CheckoutControllerExampleSettingsDefinition.ShippingAddressCollection])
            .isFalse()
        assertThat(settingsWithoutAutomaticTax[CheckoutControllerExampleSettingsDefinition.BillingAddressCollection])
            .isTrue()
        assertThat(restoredAutomaticTaxSettings[CheckoutControllerExampleSettingsDefinition.ShippingAddressCollection])
            .isFalse()
        assertThat(restoredAutomaticTaxSettings[CheckoutControllerExampleSettingsDefinition.BillingAddressCollection])
            .isTrue()
    }
}
