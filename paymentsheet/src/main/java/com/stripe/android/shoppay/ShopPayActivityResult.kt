package com.stripe.android.shoppay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface ShopPayActivityResult : Parcelable {
    @Parcelize
    data object Completed : ShopPayActivityResult

    @Parcelize
    data object Canceled : ShopPayActivityResult

    @Parcelize
    data class Failed(val error: Throwable) : ShopPayActivityResult
}
