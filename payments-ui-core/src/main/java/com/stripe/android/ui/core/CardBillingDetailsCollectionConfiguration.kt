package com.stripe.android.ui.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class CardBillingDetailsCollectionConfiguration(
    val collectName: Boolean = false,
    val collectEmail: Boolean = false,
    val collectPhone: Boolean = false,
    val address: AddressCollectionMode = AddressCollectionMode.Automatic,
) : Parcelable {

    val collectAddress: Boolean
        get() = address != AddressCollectionMode.Never

    enum class AddressCollectionMode {
        Automatic,
        Never,
        Full,
    }
}
