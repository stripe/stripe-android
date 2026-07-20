package com.stripe.android.common.nfcscan.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

internal class NfcScanningExperimentDimensionsTest {
    @Test
    fun `can_use_nfc_scanning dimension reflects availability`() {
        val metadata = PaymentMethodMetadataFactory.create()

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = metadata,
            )
        ).containsEntry("can_use_nfc_scanning", "true")

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = false,
                paymentMethodMetadata = metadata,
            )
        ).containsEntry("can_use_nfc_scanning", "false")
    }

    @Test
    fun `has_default_billing_details dimension reflects default billing details presence`() {
        val metadataWithDefaults = PaymentMethodMetadataFactory.create(
            defaultBillingDetails = PaymentSheet.BillingDetails(name = "Jane Doe"),
        )
        val metadataWithoutDefaults = metadataWithDefaults.copy(defaultBillingDetails = null)

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = metadataWithDefaults,
            )
        ).containsEntry("has_default_billing_details", "true")

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = metadataWithoutDefaults,
            )
        ).containsEntry("has_default_billing_details", "false")
    }

    @Test
    fun `billing_address_collection dimension maps address collection mode`() {
        val automaticMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            ),
        )
        val neverMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
        )
        val fullMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = automaticMetadata,
            )
        ).containsEntry("billing_address_collection", "min")

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = neverMetadata,
            )
        ).containsEntry("billing_address_collection", "none")

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = fullMetadata,
            )
        ).containsEntry("billing_address_collection", "full")
    }

    @Test
    fun `requires_contact_info_collection dimension reflects contact field collection`() {
        val noContactCollectionMetadata = PaymentMethodMetadataFactory.create()

        val nameCollectionMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )
        val phoneCollectionMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )
        val emailCollectionMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            ),
        )

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = noContactCollectionMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "false")

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = nameCollectionMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "true")

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = phoneCollectionMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "true")

        assertThat(
            NfcScanningExperimentDimensions.getDimensions(
                canUseNfcScanner = true,
                paymentMethodMetadata = emailCollectionMetadata,
            )
        ).containsEntry("requires_contact_info_collection", "true")
    }
}
