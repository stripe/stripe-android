package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MerchantCountry(val value: String) {
    @SerialName("us")
    US("us"),

    @SerialName("sg")
    SG("sg"),

    @SerialName("my")
    MY("my"),

    @SerialName("be")
    BE("be"),

    @SerialName("gb")
    GB("gb"),

    @SerialName("mex")
    MEX("mex"),

    @SerialName("au")
    AU("au"),

    @SerialName("jp")
    JP("jp"),

    @SerialName("br")
    BR("br"),

    @SerialName("fr")
    FR("fr"),

    @SerialName("th")
    TH("th"),

    @SerialName("de")
    DE("de"),

    @SerialName("it")
    IT("it")
}
