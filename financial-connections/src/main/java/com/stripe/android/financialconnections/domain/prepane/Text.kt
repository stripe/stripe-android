package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class Text(
    @SerialName("oauth_prepane")
    val oauthPrepane: OauthPrepane,
) : Parcelable
