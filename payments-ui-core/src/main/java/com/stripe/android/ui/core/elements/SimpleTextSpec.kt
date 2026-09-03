package com.stripe.android.ui.core.elements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Capitalization {
    @SerialName("none")
    None,

    @SerialName("characters")
    Characters,

    @SerialName("words")
    Words,

    @SerialName("sentences")
    Sentences
}

@Serializable
enum class KeyboardType {
    @SerialName("text")
    Text,

    @SerialName("ascii")
    Ascii,

    @SerialName("number")
    Number,

    @SerialName("phone")
    Phone,

    @SerialName("uri")
    Uri,

    @SerialName("email")
    Email,

    @SerialName("password")
    Password,

    @SerialName("number_password")
    NumberPassword
}
