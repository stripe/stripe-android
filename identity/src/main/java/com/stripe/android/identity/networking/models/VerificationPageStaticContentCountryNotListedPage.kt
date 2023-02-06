package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentCountryNotListedPage(
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: String,
    @SerialName("cancel_button_text")
    val cancelButtonText: String,
    @SerialName("id_from_other_country_text_button_text")
    val idFromOtherCountryTextButtonText: String,
    @SerialName("address_from_other_country_text_button_text")
    val addressFromOtherCountryTextButtonText: String
) : Parcelable
