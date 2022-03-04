package com.stripe.android.core.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * Internal copy of [com.stripe.android.model.StripeFileParams]. It's a public API object and can't be changed
 * without introducing backward incompatibility.
 * TODO(ccen): Move StripeFileParams to stripe-core and delete this copy during the next major version bump.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class InternalStripeFileParams constructor(
    /**
     * A file to upload. The file should follow the specifications of RFC 2388 (which defines file
     * transfers for the `multipart/form-data` protocol).
     */
    val file: File,

    /**
     * The purpose of the uploaded file. Possible values are `business_icon`, `business_logo`,
     * `customer_signature`, `dispute_evidence`, `identity_document`, `pci_document`,
     * or `tax_document_user_upload`.
     *
     * [purpose](https://stripe.com/docs/api/files/create#create_file-purpose)
     */
    val purpose: InternalStripeFilePurpose
) {
    /**
     * Optional parameters to automatically create a
     * [file link](https://stripe.com/docs/api/files/create#file_links) for the newly created file.
     *
     * [file_link_data]](https://stripe.com/docs/api/files/create#create_file-file_link_data)
     */
    private val fileLink: FileLink? = null

    /**
     * Optional parameters to automatically create a
     * [file link](https://stripe.com/docs/api/files/create#file_links) for the newly created file.
     *
     * [file_link_data]](https://stripe.com/docs/api/files/create#create_file-file_link_data)
     */
    @Parcelize
    internal data class FileLink @JvmOverloads constructor(
        /**
         * Set this to `true` to create a file link for the newly created file. Creating a link is
         * only possible when the file’s `purpose` is one of the following: `business_icon`,
         * `business_logo`, `customer_signature`, `dispute_evidence`, `pci_document`, or
         * `tax_document_user_upload`.
         *
         * [file_link_data.create](https://stripe.com/docs/api/files/create#create_file-file_link_data-create)
         */
        private val create: Boolean = false,

        /**
         * A future timestamp after which the link will no longer be usable.
         *
         * [file_link_data.expires_at](https://stripe.com/docs/api/files/create#create_file-file_link_data-expires_at)
         */
        private val expiresAt: Long? = null,

        /**
         * Set of key-value pairs that you can attach to an object. This can be useful for storing
         * additional information about the object in a structured format. Individual keys can be
         * unset by posting an empty value to them. All keys can be unset by posting an empty value
         * to metadata.
         *
         * [file_link_data.metadata](https://stripe.com/docs/api/files/create#create_file-file_link_data-metadata)
         */
        private val metadata: Map<String, String>? = null
    ) : Parcelable
}
