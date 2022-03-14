package com.stripe.android.link

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LinkActivityResult : Parcelable {

    @Parcelize
    object Success : LinkActivityResult()

    @Parcelize
    object Canceled : LinkActivityResult()

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : LinkActivityResult()
}
