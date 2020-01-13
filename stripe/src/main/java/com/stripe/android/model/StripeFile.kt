package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

/**
 * [The file object](https://stripe.com/docs/api/files/object)
 */
@Parcelize
internal data class StripeFile internal constructor(
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
    val purpose: StripeFilePurpose? = null,

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
    val type: String? = null
) : StripeModel
