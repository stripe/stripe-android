package com.stripe.android.common.nfcscan.analytics

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.PaymentSheet

internal object NfcScanningExperimentDimensions {
    fun getDimensions(
        canUseNfcScanner: Boolean,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Map<String, String> {
        val billingDetailsConfig = paymentMethodMetadata.billingDetailsCollectionConfiguration

        val billingDetailsCollection = when (billingDetailsConfig.address) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> "min"
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> "none"
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> "full"
        }

        val hasDefaultBillingDetails = paymentMethodMetadata.defaultBillingDetails != null

        val requiresContactInfoCollection = billingDetailsConfig.collectsName ||
            billingDetailsConfig.collectsPhone ||
            billingDetailsConfig.collectsEmail

        return mapOf(
            "can_use_nfc_scanning" to canUseNfcScanner.toString(),
            "has_default_billing_details" to hasDefaultBillingDetails.toString(),
            "billing_address_collection" to billingDetailsCollection,
            "requires_contact_info_collection" to requiresContactInfoCollection.toString(),
        )
    }
}
