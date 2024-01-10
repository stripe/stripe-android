package com.stripe.android.ui.core.elements.autocomplete.model

import android.content.Context
import android.os.Build
import androidx.annotation.RestrictTo
import java.util.Locale

// Largely duplicated from
// https://git.corp.stripe.com/stripe-internal/stripe-js-v3/blob/1c111621/src/elements/inner/autocomplete_suggestions/utils/transformGoogleToStripeAddress.ts#L248-L325

internal data class AddressLine1(
    var streetNumber: String? = null,
    var route: String? = null,
    var subLocalityLevel2: String? = null,
    var subLocalityLevel3: String? = null,
    var subLocalityLevel4: String? = null
)

internal data class Address(
    var locality: String? = null,
    var country: String? = null,
    var addressLine1: String? = null,
    var addressLine2: String? = null,
    var administrativeArea: String? = null,
    var dependentLocality: String? = null,
    var postalCode: String? = null
)

// These countries will have address line 1 that takes the format
// of "King Street 123" instead of "123 King Street"
// Reference for country formats:
// https://docs.google.com/spreadsheets/d/1tIZO0-Iqvs_8CA9UL3S9qYvoTzjPdrHZr-hdetz6Uuo/edit#gid=696373988
internal val STREET_NAME_FIRST_COUNTRIES = setOf(
    "BE",
    "BR",
    "CH",
    "DE",
    "ES",
    "ID",
    "IT",
    "MX",
    "NL",
    "NO",
    "PL",
    "RU",
    "SE",
)

internal fun Place.filter(type: Place.Type): AddressComponent? {
    return addressComponents?.find { it.types.contains(type.value) }
}

internal fun composeAddressLine1(
    context: Context,
    addressLine1: AddressLine1,
    address: Address
): String {
    val streetNumber = addressLine1.streetNumber ?: ""
    val streetName = addressLine1.route ?: ""
    val locality = address.locality
    val countryCode = address.country

    return if (countryCode == "JP") {
        val premise = address.addressLine2
        composeJapaneseAddressLine1(context, addressLine1, locality, premise)
    } else if (streetNumber.isNotBlank() || streetName.isNotBlank()) {
        // Depending on the country we'll want either
        // "123 King Street" or "King Street 123"
        return if (STREET_NAME_FIRST_COUNTRIES.contains(countryCode)) {
            "$streetName $streetNumber".trim()
        } else {
            "$streetNumber $streetName".trim()
        }
    } else {
        ""
    }
}

internal fun composeJapaneseAddressLine1(
    context: Context,
    addressLine1: AddressLine1,
    localityComponent: String?,
    premiseComponent: String?
): String {
    val districtBlockNumberAllExist =
        addressLine1.subLocalityLevel2 != null &&
            addressLine1.subLocalityLevel3 != null &&
            addressLine1.subLocalityLevel4 != null
    val district = addressLine1.subLocalityLevel3
    val block = addressLine1.subLocalityLevel4
    val buildingNumber = premiseComponent ?: ""
    val locality = localityComponent ?: ""
    val municipality = addressLine1.subLocalityLevel2

    val composite: String

    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
    } else {
        context.resources.configuration.locale
    }

    if (locale == Locale.JAPANESE) {
        // In Japanese, address line 1 components should be in this order:
        // `{locality}{municipality}{district}{block}-{buildingNumber}`
        val districtBlockNumber = if (districtBlockNumberAllExist) {
            "$district$block-$buildingNumber"
        } else {
            ""
        }
        composite = "$locality$municipality$districtBlockNumber"
    } else {
        // In Latin script, address line 1 components should be in this order:
        // `{district}-{block}-{buildingNumber} {municipality} {locality}`
        val districtBlockNumber = if (districtBlockNumberAllExist) {
            "$district-$block-$buildingNumber"
        } else {
            ""
        }
        composite = "$districtBlockNumber $municipality $locality"
    }

    return composite
}

internal fun Address.combineDependentLocalityWithLine2(): Address {
    val newAddress = copy()
    if (dependentLocality != null) {
        newAddress.addressLine2 = if (addressLine2 != null) {
            "$addressLine2, $dependentLocality"
        } else {
            dependentLocality
        }
    }
    return newAddress
}

internal fun Address.modifyStripeAddressByCountry(place: Place): Address {
    val administrativeAreaLevel2 = place.filter(Place.Type.ADMINISTRATIVE_AREA_LEVEL_2)?.shortName
    val administrativeAreaLevel1 = place.filter(Place.Type.ADMINISTRATIVE_AREA_LEVEL_1)?.longName
    var newAddress = copy()
    when (country) {
        "IE" -> {
            if (administrativeAreaLevel1 != null) {
                newAddress.administrativeArea = administrativeAreaLevel1
                newAddress = newAddress.combineDependentLocalityWithLine2()
            }
        }
        // For Japanese addresses, the "premise" is the building number and not
        // address line 2, so clear addressLine2 if it exists
        "JP" -> {
            newAddress.addressLine2 = null
        }
        // Turkish districts correspond to the "locality" field in
        // the address form, and districts are in "administrative_area_level_2"
        // Brazilian addresses have the locality in "administrative_area_level_2"
        "TR", "BR" -> {
            if (locality == null && administrativeAreaLevel2 != null) {
                newAddress.locality = administrativeAreaLevel2
            }
            newAddress = newAddress.combineDependentLocalityWithLine2()
        }
        // Most countries contain the province in "administrative_area_level_1"
        // However, Spanish and Italian provinces are in
        // "administrative_area_level_2" and "administrative_area_level_1"
        // contains the region (eg. the autonomous community, like Basque), which
        // isn't used in mailing addresses.
        "ES", "IT" -> {
            if (administrativeAreaLevel2 != null) {
                newAddress.administrativeArea = administrativeAreaLevel2
            }
        }
        "MX", "MY", "PH", "ZA" -> {
            newAddress = newAddress.combineDependentLocalityWithLine2()
        }
    }

    return newAddress
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Place.transformGoogleToStripeAddress(
    context: Context
): com.stripe.android.model.Address {
    var address = Address()
    val addressLine1 = AddressLine1()

    addressComponents?.forEach { field ->
        when (field.types[0]) {
            Place.Type.STREET_NUMBER.value -> {
                addressLine1.streetNumber = field.longName
            }
            Place.Type.ROUTE.value -> {
                addressLine1.route = field.longName
            }
            Place.Type.PREMISE.value -> {
                address.addressLine2 = field.longName
            }
            Place.Type.LOCALITY.value,
            Place.Type.SUBLOCALITY.value,
            Place.Type.POSTAL_TOWN.value -> {
                address.locality = field.longName
            }
            Place.Type.ADMINISTRATIVE_AREA_LEVEL_1.value -> {
                address.administrativeArea = field.shortName
            }
            Place.Type.ADMINISTRATIVE_AREA_LEVEL_3.value -> {
                if (address.locality == null) {
                    address.locality = field.longName
                }
            }
            Place.Type.ADMINISTRATIVE_AREA_LEVEL_2.value -> {
                if (address.administrativeArea == null && address.dependentLocality == null) {
                    address.dependentLocality = field.longName
                } else {
                    address.administrativeArea = field.shortName
                }
            }
            Place.Type.NEIGHBORHOOD.value -> {
                if (address.locality == null) {
                    address.locality = field.longName
                } else {
                    address.dependentLocality = field.longName
                }
            }
            Place.Type.POSTAL_CODE.value -> {
                address.postalCode = field.longName
            }
            Place.Type.COUNTRY.value -> {
                address.country = field.shortName
            }
            Place.Type.SUBLOCALITY_LEVEL_1.value -> {
                if (address.locality == null) {
                    address.dependentLocality = field.longName
                } else {
                    address.locality = field.longName
                }
            }
            Place.Type.SUBLOCALITY_LEVEL_2.value -> {
                addressLine1.subLocalityLevel2 = field.longName
            }
            Place.Type.SUBLOCALITY_LEVEL_3.value -> {
                addressLine1.subLocalityLevel3 = field.longName
            }
            Place.Type.SUBLOCALITY_LEVEL_4.value -> {
                addressLine1.subLocalityLevel4 = field.longName
            }
        }
    }

    address.addressLine1 = composeAddressLine1(context, addressLine1, address)
    address = address.modifyStripeAddressByCountry(this)

    return com.stripe.android.model.Address.Builder()
        .setLine1(address.addressLine1)
        .setLine2(address.addressLine2)
        .setCity(address.locality)
        .setState(address.administrativeArea)
        .setCountry(address.country)
        .setPostalCode(address.postalCode)
        .build()
}
