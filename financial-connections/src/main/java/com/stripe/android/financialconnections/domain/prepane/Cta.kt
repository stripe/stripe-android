package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import com.stripe.android.financialconnections.model.Image
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class Cta(
    @SerialName("icon")
    val icon: Image,
    @SerialName("text")
    val text: String
) : Parcelable
