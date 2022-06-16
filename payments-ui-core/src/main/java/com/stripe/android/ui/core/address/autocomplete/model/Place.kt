package com.stripe.android.ui.core.address.autocomplete.model

data class FindAutocompletePredictionsResponse(
    val autocompletePredictions: List<AutocompletePrediction>
)

data class AutocompletePrediction(
    val primaryText: String,
    val secondaryText: String,
    val placeId: String
)

data class FetchPlaceResponse(
    val place: Place
)

data class Place(
    val addressComponents: List<AddressComponent>?
) {
    enum class Type(val value: String) {
        STREET_NUMBER("street_number"),
        ROUTE("route"),
        LOCALITY("locality"),
        SUBLOCALITY("sublocality"),
        ADMINISTRATIVE_AREA_LEVEL_1("administrative_area_level_1"),
        COUNTRY("country"),
        POSTAL_CODE("postal_code")
    }
}

data class AddressComponent(
    val name: String,
    val types: List<String>
)
