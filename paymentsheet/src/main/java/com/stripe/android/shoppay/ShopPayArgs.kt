package com.stripe.android.shoppay

import android.os.Parcelable
import com.stripe.android.paymentsheet.WalletConfiguration
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ShopPayArgs(
    val checkoutUrl: String,
) : Parcelable
