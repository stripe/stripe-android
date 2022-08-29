package com.stripe.android.ui.core.address

import android.os.Parcelable
import com.stripe.android.model.Address
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShippingAddress(
    val name: String?,
    val phone: String?,
    val address: Address?,
    val sameAsShippingEnabled: Boolean?
) : Parcelable
