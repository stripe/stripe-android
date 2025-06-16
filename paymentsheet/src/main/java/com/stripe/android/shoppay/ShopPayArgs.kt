package com.stripe.android.shoppay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShopPayArgs(
    val checkoutUrl: String,
) : Parcelable
