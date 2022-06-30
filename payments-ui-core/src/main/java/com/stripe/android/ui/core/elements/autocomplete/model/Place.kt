package com.stripe.android.ui.core.elements.autocomplete.model

import android.text.SpannableString

internal data class FindAutocompletePredictionsResponse(
    val autocompletePredictions: List<AutocompletePrediction>
)

internal data class AutocompletePrediction(
    val primaryText: SpannableString,
    val secondaryText: SpannableString,
    val placeId: String
)

internal data class FetchPlaceResponse(
    val place: Place
)

internal data class Place(
    val addressComponents: List<AddressComponent>?
) {
    enum class Type(val value: String) {
        ADMINISTRATIVE_AREA_LEVEL_1("administrative_area_level_1"),
        ADMINISTRATIVE_AREA_LEVEL_2("administrative_area_level_2"),
        ADMINISTRATIVE_AREA_LEVEL_3("administrative_area_level_3"),
        ADMINISTRATIVE_AREA_LEVEL_4("administrative_area_level_4"),
        COUNTRY("country"),
        LOCALITY("locality"),
        NEIGHBORHOOD("neighborhood"),
        POSTAL_TOWN("postal_town"),
        POSTAL_CODE("postal_code"),
        PREMISE("premise"),
        ROUTE("route"),
        STREET_NUMBER("street_number"),
        SUBLOCALITY("sublocality"),
        SUBLOCALITY_LEVEL_2("sublocality_level_2"),
        SUBLOCALITY_LEVEL_3("sublocality_level_3"),
        SUBLOCALITY_LEVEL_4("sublocality_level_4"),
    }
}

internal data class AddressComponent(
    val shortName: String?,
    val longName: String,
    val types: List<String>
) {
    fun contains(type: Place.Type): Boolean {
        return types.contains(type.value)
    }
}
