package com.stripe.android.core.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Internal copy of [com.stripe.android.model.StripeFile]. It's a public API object and can't be changed
 * without introducing backward incompatibility.
 * TODO(ccen): Move StripeFile to stripe-core and delete this copy during the next major version bump.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class InternalStripeFile constructor(
    /**
     * Unique identifier for the object.
     *
     * [id](https://stripe.com/docs/api/files/object#file_object-id)
     */
    val id: String? = null,

    /**
     * Time at which the object was created. Measured in seconds since the Unix epoch.
     *
     * [created](https://stripe.com/docs/api/files/object#file_object-created)
     */
    val created: Long? = null,

    /**
     * A filename for the file, suitable for saving to a filesystem.
     *
     * [filename](https://stripe.com/docs/api/files/object#file_object-filename)
     */
    val filename: String? = null,

    /**
     * The purpose of the file. Possible values are `business_icon`, `business_logo`,
     * `customer_signature`, `dispute_evidence`, `identity_document`, `pci_document`,
     * or `tax_document_user_upload`.
     *
     * [purpose](https://stripe.com/docs/api/files/object#file_object-purpose)
     */
    val purpose: InternalStripeFilePurpose? = null,

    /**
     * The size in bytes of the file object.
     *
     * [size](https://stripe.com/docs/api/files/object#file_object-size)
     */
    val size: Int? = null,

    /**
     * A user friendly title for the document.
     *
     * [title](https://stripe.com/docs/api/files/object#file_object-title)
     */
    val title: String? = null,

    /**
     * The type of the file returned (e.g., csv, pdf, jpg, or png).
     *
     * [type](https://stripe.com/docs/api/files/object#file_object-type)
     */
    val type: String? = null,

    /**
     * The URL from which the file can be downloaded using your live secret API key.
     *
     * [url](https://stripe.com/docs/api/files/object#file_object-url)
     */
    val url: String? = null
) : StripeModel
