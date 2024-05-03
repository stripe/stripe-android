package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.BillingAddressConfig.Format
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
import org.junit.Test

class BillingDetailsCollectionConfigurationTest {

    @Test
    fun `Creates correct Google Pay billing address config with default collection configuration`() {
        val collectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration()

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
        val collectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(address = Full)

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
        val collectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
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
        val collectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(phone = Always)

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
