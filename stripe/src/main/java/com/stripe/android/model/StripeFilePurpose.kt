package com.stripe.android.model

/**
 * The purpose of the uploaded file. Possible values are `business_icon`, `business_logo`,
 * `customer_signature`, `dispute_evidence`, `identity_document`, `pci_document`,
 * or `tax_document_user_upload`.
 *
 * [purpose](https://stripe.com/docs/api/files/create#create_file-purpose)
*/
enum class StripeFilePurpose(internal val code: String) {
    BusinessIcon("business_icon"),
    BusinessLogo("business_logo"),
    CustomerSignature("customer_signature"),
    DisputeEvidence("dispute_evidence"),
    IdentityDocument("identity_document"),
    PciDocument("pci_document"),
    TaxDocumentUserUpload("tax_document_user_upload");

    internal companion object {
        fun fromCode(code: String?): StripeFilePurpose? {
            return values().first { it.code == code }
        }
    }
}
