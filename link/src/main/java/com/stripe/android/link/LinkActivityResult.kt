package com.stripe.android.link

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LinkActivityResult : Parcelable {

    @Parcelize
    object Success : LinkActivityResult()
}
