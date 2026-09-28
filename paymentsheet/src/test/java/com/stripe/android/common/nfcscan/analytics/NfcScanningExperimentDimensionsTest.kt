package com.stripe.android.common.nfcscan.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

internal class NfcScanningExperimentDimensionsTest {
    @Test
    fun `can_use_nfc_scanning is true when NFC scanner is available`() {
        val metadata = PaymentMethodMetadataFactory.create()

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = metadata,
            )
        ).containsEntry("can_use_nfc_scanning", "true")
    }

    @Test
    fun `can_use_nfc_scanning is false when NFC scanner is unavailable`() {
        val metadata = PaymentMethodMetadataFactory.create()

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = false,
                paymentMethodMetadata = metadata,
            )
        ).containsEntry("can_use_nfc_scanning", "false")
    }

    @Test
    fun `has_default_billing_details is true when default billing details is available`() {
        val metadataWithDefaults = PaymentMethodMetadataFactory.create(
            defaultBillingDetails = PaymentSheet.BillingDetails(name = "Jane Doe"),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = metadataWithDefaults,
            )
        ).containsEntry("has_default_billing_details", "true")
    }

    @Test
    fun `has_default_billing_details is false when default billing details is empty`() {
        val metadataWithoutDefaults = PaymentMethodMetadataFactory.create(
            defaultBillingDetails = PaymentSheet.BillingDetails()
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = metadataWithoutDefaults,
            )
        ).containsEntry("has_default_billing_details", "false")
    }

    @Test
    fun `has_default_billing_details is false when default billing details is null`() {
        val metadataWithoutDefaults = PaymentMethodMetadataFactory.create().copy(defaultBillingDetails = null)

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = metadataWithoutDefaults,
            )
        ).containsEntry("has_default_billing_details", "false")
    }

    @Test
    fun `billing_address_collection is 'min' when address collection is 'Automatic'`() {
        val automaticMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = automaticMetadata,
            )
        ).containsEntry("billing_address_collection", "min")
    }

    @Test
    fun `billing_address_collection is 'none' when address collection is 'Never'`() {
        val automaticMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = automaticMetadata,
            )
        ).containsEntry("billing_address_collection", "none")
    }

    @Test
    fun `billing_address_collection is 'full' when address collection is 'Full'`() {
        val automaticMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = automaticMetadata,
            )
        ).containsEntry("billing_address_collection", "full")
    }

    @Test
    fun `requires_contact_info_collection is false when none of contact fields are always`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "false")
    }

    @Test
    fun `requires_contact_info_collection is true when name field is always collected`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "true")
    }

    @Test
    fun `requires_contact_info_collection is true when email field is always collected`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "true")
    }

    @Test
    fun `requires_contact_info_collection is true when phone field is always collected`() {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "true")
    }
}
