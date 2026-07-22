package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import kotlin.test.Test

@OptIn(CheckoutSessionPreview::class)
internal class BillingDetailsCollectionConfigurationMapperTest {

    @Test
    fun `default owned config maps to PaymentSheet defaults with attachDefaults forced true`() {
        val mapped = BillingDetailsCollectionConfiguration().build().asPaymentSheet()
        assertThat(mapped)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration(attachDefaultsToPaymentMethod = true))
    }

    @Test
    fun `CollectionMode Automatic maps to Automatic`() {
        val mapped = BillingDetailsCollectionConfiguration()
            .name(BillingDetailsCollectionConfiguration.CollectionMode.Automatic)
            .build()
            .asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic)
    }

    @Test
    fun `CollectionMode Never maps to Never`() {
        val mapped = BillingDetailsCollectionConfiguration()
            .name(BillingDetailsCollectionConfiguration.CollectionMode.Never)
            .build()
            .asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never)
    }

    @Test
    fun `CollectionMode Always maps to Always`() {
        val mapped = BillingDetailsCollectionConfiguration()
            .name(BillingDetailsCollectionConfiguration.CollectionMode.Always)
            .build()
            .asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `all CollectionMode fields map 1 to 1`() {
        val mapped = BillingDetailsCollectionConfiguration()
            .name(BillingDetailsCollectionConfiguration.CollectionMode.Always)
            .phone(BillingDetailsCollectionConfiguration.CollectionMode.Never)
            .email(BillingDetailsCollectionConfiguration.CollectionMode.Always)
            .build()
            .asPaymentSheet()
        assertThat(mapped.name)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always)
        assertThat(mapped.phone)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never)
        assertThat(mapped.email)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always)
    }

    @Test
    fun `AddressCollectionMode Automatic maps to Automatic`() {
        val mapped = BillingDetailsCollectionConfiguration()
            .address(BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic)
            .build()
            .asPaymentSheet()
        assertThat(mapped.address)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic)
    }

    @Test
    fun `AddressCollectionMode Full maps to Full`() {
        val mapped = BillingDetailsCollectionConfiguration()
            .address(BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
            .build()
            .asPaymentSheet()
        assertThat(mapped.address)
            .isEqualTo(PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
    }

    @Test
    fun `reconcile upgrades Automatic to Full when the session requires a billing address`() {
        val state = BillingDetailsCollectionConfiguration()
            .address(BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic)
            .build()

        val reconciled = state.reconcile(requiresBillingAddress = true)

        assertThat(reconciled.address).isEqualTo(BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
    }

    @Test
    fun `reconcile leaves Full unchanged when the session requires a billing address`() {
        val state = BillingDetailsCollectionConfiguration()
            .address(BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
            .build()

        val reconciled = state.reconcile(requiresBillingAddress = true)

        assertThat(reconciled.address).isEqualTo(BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
    }

    @Test
    fun `reconcile leaves Automatic unchanged when the session does not require a billing address`() {
        val state = BillingDetailsCollectionConfiguration()
            .address(BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic)
            .build()

        val reconciled = state.reconcile(requiresBillingAddress = false)

        assertThat(reconciled.address)
            .isEqualTo(BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic)
    }

    @Test
    fun `reconcile leaves Full unchanged when the session does not require a billing address`() {
        val state = BillingDetailsCollectionConfiguration()
            .address(BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
            .build()

        val reconciled = state.reconcile(requiresBillingAddress = false)

        assertThat(reconciled.address).isEqualTo(BillingDetailsCollectionConfiguration.AddressCollectionMode.Full)
    }
}
