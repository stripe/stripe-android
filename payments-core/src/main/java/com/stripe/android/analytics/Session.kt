package com.stripe.android.analytics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface Session : Parcelable {
    @Parcelize
    data class Owner(val id: String) : Session

    @Parcelize
    data object Observer : Session
}
