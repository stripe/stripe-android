package com.stripe.android.identity.networking.models

import android.os.Parcelable
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.serializers.CountryListSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentIndividualPage(
    @SerialName("address_countries")
    @Serializable(with = CountryListSerializer::class)
    val addressCountries: List<Country>,
    @SerialName("button_text")
    val buttonText: String,
    @SerialName("title")
    val title: String,
    @SerialName("id_number_countries")
    @Serializable(with = CountryListSerializer::class)
    val idNumberCountries: List<Country>
) : Parcelable
