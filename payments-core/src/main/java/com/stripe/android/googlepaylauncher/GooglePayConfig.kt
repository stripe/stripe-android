package com.stripe.android.googlepaylauncher

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class GooglePayConfig(
    var environment: GooglePayEnvironment,

    /**
     * Total monetary value of the transaction.
     *
     * The value of this field is represented in the [smallest currency unit](https://stripe.com/docs/currencies#zero-decimal).
     * For example, when [currencyCode] is `"USD"`, a value of `100` represents 100 cents ($1.00).
     */
    internal var amount: Int?,

    /**
     * ISO 3166-1 alpha-2 country code where the transaction is processed.
     */
    internal var countryCode: String,

    /**
     * ISO 4217 alphabetic currency code.
     */
    internal var currencyCode: String,

    /**
     * Set to true to request an email address.
     */
    internal var isEmailRequired: Boolean = false,

    /**
     * Merchant name encoded as UTF-8.
     */
    internal var merchantName: String? = null,

    /**
     * A unique ID that identifies a transaction attempt. Merchants may use an existing ID or
     * generate a specific one for Google Pay transaction attempts. This field is required
     * when you send callbacks to the Google Transaction Events API.
     */
    internal var transactionId: String? = null
) : Parcelable
