package com.stripe.android.financialconnections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class Image(

    @SerialName(value = "default") val default: String?

) : Parcelable
