package com.stripe.android.core.model

import androidx.annotation.RestrictTo

/**
 * Internal copy of [com.stripe.android.model.StripeFilePurpose]. It's a public API object and can't be changed
 * without introducing backward incompatibility.
 * TODO(ccen): Move StripeFilePurpose to stripe-core and delete this copy during the next major version bump.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class InternalStripeFilePurpose(val code: String) {
    BusinessIcon("business_icon"),
    BusinessLogo("business_logo"),
    CustomerSignature("customer_signature"),
    DisputeEvidence("dispute_evidence"),
    IdentityDocument("identity_document"),
    PciDocument("pci_document"),
    TaxDocumentUserUpload("tax_document_user_upload"),
    IdentityPrivate("identity_private");

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromCode(code: String?): InternalStripeFilePurpose? {
            return values().first { it.code == code }
        }
    }
}
