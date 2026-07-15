package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.PaymentElement.Configuration.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
internal class BillingDetailsCollectionConfigurationMapperTest {

    @Test
    fun `default owned config maps to default PaymentSheet config`() {
        val mapped = BillingDetailsCollectionConfiguration().asPaymentSheet()
        assertThat(mapped).isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration())
    }

    @Test
    fun `CollectionMode Automatic maps to Automatic`() {
        val mapped = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
        ).asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic)
    }

    @Test
    fun `CollectionMode Never maps to Never`() {
        val mapped = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
        ).asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never)
    }

    @Test
    fun `CollectionMode Always maps to Always`() {
        val mapped = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
        ).asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `all CollectionMode fields map 1 to 1`() {
        val mapped = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
        ).asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always)
        assertThat(mapped.phone)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never)
        assertThat(mapped.email)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `AddressCollectionMode Automatic maps to Automatic`() {
        val mapped = BillingDetailsCollectionConfiguration(
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        ).asPaymentSheet()
        assertThat(mapped.address)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic)
    }

    @Test
    fun `AddressCollectionMode Never maps to Never`() {
        val mapped = BillingDetailsCollectionConfiguration(
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
        ).asPaymentSheet()
        assertThat(mapped.address)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never)
    }

    @Test
    fun `AddressCollectionMode Full maps to Full`() {
        val mapped = BillingDetailsCollectionConfiguration(
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        ).asPaymentSheet()
        assertThat(mapped.address)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
    }

    @Test
    fun `attachDefaultsToPaymentMethod false passes through`() {
        val mapped = BillingDetailsCollectionConfiguration(
            attachDefaultsToPaymentMethod = false,
        ).asPaymentSheet()
        assertThat(mapped.attachDefaultsToPaymentMethod).isFalse()
    }

    @Test
    fun `attachDefaultsToPaymentMethod true passes through`() {
        val mapped = BillingDetailsCollectionConfiguration(
            attachDefaultsToPaymentMethod = true,
        ).asPaymentSheet()
        assertThat(mapped.attachDefaultsToPaymentMethod).isTrue()
    }
}
