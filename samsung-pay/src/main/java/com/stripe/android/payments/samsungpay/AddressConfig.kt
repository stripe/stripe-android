package com.stripe.android.payments.samsungpay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration for address collection in the Samsung Pay sheet.
 */
@Parcelize
class AddressConfig(
    val format: Format = Format.None,
    val isPhoneNumberRequired: Boolean = false,
) : Parcelable {

    enum class Format {
        /** No address collection. */
        None,
        /** Billing address only. */
        BillingOnly,
        /** Shipping address only. */
        ShippingOnly,
        /** Both billing and shipping. */
        BillingAndShipping,
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddressConfig) return false
        return format == other.format && isPhoneNumberRequired == other.isPhoneNumberRequired
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + isPhoneNumberRequired.hashCode()
        return result
    }
}
