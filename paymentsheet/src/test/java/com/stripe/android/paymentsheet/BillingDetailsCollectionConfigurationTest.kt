package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
import com.stripe.android.elements.BillingDetailsCollectionConfiguration.CollectionMode.Always
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.BillingAddressConfig.Format
import org.junit.Test

class BillingDetailsCollectionConfigurationTest {

    @Test
    fun `Creates correct Google Pay billing address config with default collection configuration`() {
        val collectionConfiguration = BillingDetailsCollectionConfiguration()

        val billingAddressConfig = collectionConfiguration.toBillingAddressConfig()

        assertThat(billingAddressConfig).isEqualTo(
            GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = false,
                format = Format.Min,
                isPhoneNumberRequired = false,
            )
        )
    }

    @Test
    fun `Creates correct Google Pay billing address config when collecting full address`() {
        val collectionConfiguration = BillingDetailsCollectionConfiguration(address = Full)

        val billingAddressConfig = collectionConfiguration.toBillingAddressConfig()

        assertThat(billingAddressConfig).isEqualTo(
            GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = true,
                format = Format.Full,
                isPhoneNumberRequired = false,
            )
        )
    }

    @Test
    fun `Creates correct Google Pay billing address config when collecting full address and phone`() {
        val collectionConfiguration = BillingDetailsCollectionConfiguration(
            phone = Always,
            address = Full,
        )

        val billingAddressConfig = collectionConfiguration.toBillingAddressConfig()

        assertThat(billingAddressConfig).isEqualTo(
            GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = true,
                format = Format.Full,
                isPhoneNumberRequired = true,
            )
        )
    }

    @Test
    fun `Creates correct Google Pay billing address config when collecting only phone`() {
        val collectionConfiguration = BillingDetailsCollectionConfiguration(phone = Always)

        val billingAddressConfig = collectionConfiguration.toBillingAddressConfig()

        assertThat(billingAddressConfig).isEqualTo(
            GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = true,
                format = Format.Min,
                isPhoneNumberRequired = true,
            )
        )
    }
}
