package com.stripe.android.paymentsheet.addresscollection

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class CollectedAddress(
    val name: String? = null,
    val company: String? = null,
    val city: String? = null,
    val country: String? = null,
    val line1: String? = null,
    val line2: String? = null,
    val postalCode: String? = null,
    val state: String? = null,
    val phoneNumber: String? = null
): Parcelable {
    companion object {
        const val KEY = "CollectedAddress"
    }
}
