package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
internal data class Display(
    @SerialName("text")
    val text: Text
) : Parcelable
